package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.event.DeleteInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.InvoicePaymentRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.InvoicePaymentResponse
import com.cogoport.ares.model.settlement.event.InvoiceBalance
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Date
import javax.transaction.Transactional

@Singleton
open class AccountUtilizationServiceImpl : AccountUtilizationService {

    @Inject
    lateinit var accUtilRepository: AccountUtilizationRepository

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var accountUtilizationConverter: AccountUtilizationMapper

    @Inject
    lateinit var auditService: AuditService

    /**
     * @param accUtilizationRequestList
     * @return listOf CreateInvoiceResponse
     */
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {
        val responseList = mutableListOf<CreateInvoiceResponse>()
        for (accUtilizationRequest in accUtilizationRequestList) {
            if (accUtilizationRequest.migrated == null) {
                accUtilizationRequest.migrated = false
            }

            if (!Utilities.isInvoiceAccountType(accUtilizationRequest.accType!!)) {
                responseList.add(
                    CreateInvoiceResponse(
                        0L,
                        accUtilizationRequest.documentNo,
                        false,
                        AresError.ERR_1202.message
                    )
                )
                continue
            }
            if (accUtilRepository.isDocumentNumberExists(
                    accUtilizationRequest.documentNo,
                    accUtilizationRequest.accType!!.name
                )
            ) {
                responseList.add(
                    CreateInvoiceResponse(
                        0L,
                        accUtilizationRequest.documentNo,
                        false,
                        AresError.ERR_1201.message
                    )
                )
                continue
            }

            val acUtilization = accountUtilizationConverter.convertToEntity(accUtilizationRequest)
            acUtilization.createdAt = Timestamp.from(Instant.now())
            acUtilization.updatedAt = Timestamp.from(Instant.now())
            acUtilization.signFlag = getSignFlag(accUtilizationRequest.accType!!)
            acUtilization.accCode = AresModelConstants.AP_ACCOUNT_CODE

            if (accUtilizationRequest.accMode == AccMode.AR) {
                acUtilization.accCode = AresModelConstants.AR_ACCOUNT_CODE
            }
            val accUtilRes = accUtilRepository.save(acUtilization)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                    objectId = accUtilRes.id,
                    actionName = AresConstants.CREATE,
                    data = accUtilRes,
                    performedBy = accUtilizationRequest.performedBy.toString(),
                    performedByUserType = accUtilizationRequest.performedByType
                )
            )
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            responseList.add(
                CreateInvoiceResponse(
                    accUtilRes.id!!,
                    accUtilizationRequest.documentNo,
                    true,
                    Messages.SUCCESS_INVOICE_CREATION
                )
            )
            if (accUtilRes.payCurr.compareTo(BigDecimal.ZERO) == 1 && (accUtilRes.accType == AccountType.SINV || accUtilRes.accType == AccountType.SREIMB)) {
                aresKafkaEmitter.emitInvoiceBalance(
                    UpdateInvoiceBalanceEvent(
                        invoiceBalance = InvoiceBalance(
                            invoiceId = accUtilRes.documentNo,
                            balanceAmount = (accUtilRes.amountCurr - accUtilRes.payCurr),
                            performedBy = accUtilizationRequest.performedBy,
                            performedByUserType = accUtilizationRequest.performedByType,
                            paymentStatus = Utilities.getPaymentStatus(accUtilRes),
                        ), knockoffDocuments = null,
                    )
                )
            }
        }
        return responseList
    }

    /**
     * Add account utilization
     * @param accUtilizationRequest
     * @return CreateInvoiceResponse
     */
    override suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse {
        val accUtilizationList = mutableListOf<AccUtilizationRequest>()
        accUtilizationList.add(accUtilizationRequest)
        val listResponse = add(accUtilizationList)
        try {
            emitDashboardAndOutstandingEvent(accUtilizationRequest)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        // emitAccUtilizationToDemeter(accUtilizationRequest)
        return listResponse[0]
    }

    override suspend fun delete(request: DeleteInvoiceRequest): Boolean {
        var result = false
        for (obj in request.data) {

            val accountUtilization = accUtilRepository.findRecord(obj.first, obj.second)
//                ?: throw AresException(AresError.ERR_1005, obj.first.toString())

            if (accountUtilization != null) {
                if (Utilities.isPayAccountType(accountUtilization.accType)) {
                    throw AresException(AresError.ERR_1202, obj.first.toString())
                }
                //            if (accountUtilization.documentStatus == DocumentStatus.FINAL) {
                //                throw AresException(AresError.ERR_1204, obj.first.toString())
                //            }
                accUtilRepository.deleteInvoiceUtils(accountUtilization.id!!)
                auditService.createAudit(
                    AuditRequest(
                        objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                        objectId = accountUtilization.id,
                        actionName = AresConstants.DELETE,
                        data = accountUtilization,
                        performedBy = request.performedBy.toString(),
                        performedByUserType = request.performedByUserType
                    )
                )
                Client.removeDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accountUtilization.id.toString())
                val accUtilizationRequest = accountUtilizationConverter.convertToModel(accountUtilization)
                try {
                    emitDashboardAndOutstandingEvent(accUtilizationRequest)
                } catch (e: Exception) {
                    logger().error(e.stackTraceToString())
                }
                // emitAccUtilizationToDemeter(accUtilizationRequest)
                result = true
            }
        }
        return result
    }

    /**
     * Updates Invoice in account utilization
     * @param UpdateInvoiceRequest
     */
    override suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest) {

        val accountUtilization =
            accUtilRepository.findRecord(updateInvoiceRequest.documentNo, updateInvoiceRequest.accType.name)
                ?: throw AresException(AresError.ERR_1005, updateInvoiceRequest.documentNo.toString())

        if (accountUtilization.payCurr.compareTo(BigDecimal.ZERO) != 0 && accountUtilization.payLoc.compareTo(BigDecimal.ZERO) != 0) {
            val paymentEntry = accUtilRepository.findPaymentsByDocumentNo(updateInvoiceRequest.documentNo)
            if (updateInvoiceRequest.currAmount > accountUtilization.amountCurr && updateInvoiceRequest.ledAmount > accountUtilization.amountLoc) {
                amountGreaterThanExistingRecord(updateInvoiceRequest, accountUtilization, paymentEntry)
            } else {
                amountLessThanExistingRecord(updateInvoiceRequest, paymentEntry, accountUtilization)
            }
        }
        val paymentEntry = accUtilRepository.findPaymentsByDocumentNo(updateInvoiceRequest.documentNo)
        var newPayCurr: BigDecimal = 0.toBigDecimal()
        var newPayLoc: BigDecimal = 0.toBigDecimal()
        for (payments in paymentEntry) {
            newPayCurr += payments?.payCurr!!
            newPayLoc += payments.payLoc
        }

        accountUtilization.transactionDate = updateInvoiceRequest.transactionDate
        accountUtilization.documentValue = updateInvoiceRequest.documentValue
        accountUtilization.dueDate = updateInvoiceRequest.dueDate
        accountUtilization.documentStatus = updateInvoiceRequest.docStatus
        accountUtilization.entityCode = updateInvoiceRequest.entityCode
        accountUtilization.currency = updateInvoiceRequest.currency
        accountUtilization.ledCurrency = updateInvoiceRequest.ledCurrency
        accountUtilization.amountCurr = updateInvoiceRequest.currAmount
        accountUtilization.amountLoc = updateInvoiceRequest.ledAmount
        accountUtilization.accType = updateInvoiceRequest.accType
        accountUtilization.updatedAt = Timestamp.from(Instant.now())
        accountUtilization.payCurr = newPayCurr
        accountUtilization.payLoc = newPayLoc

        accUtilRepository.update(accountUtilization)

        accountUtilization.orgSerialId = updateInvoiceRequest.orgSerialId ?: accountUtilization.orgSerialId
        accountUtilization.sageOrganizationId = updateInvoiceRequest.sageOrganizationId ?: accountUtilization.sageOrganizationId
        accountUtilization.organizationId = updateInvoiceRequest.organizationId ?: accountUtilization.organizationId
        accountUtilization.taggedOrganizationId = updateInvoiceRequest.taggedOrganizationId ?: accountUtilization.taggedOrganizationId
        accountUtilization.tradePartyMappingId = updateInvoiceRequest.tradePartyMappingId ?: accountUtilization.tradePartyMappingId
        accountUtilization.organizationName = updateInvoiceRequest.organizationName ?: accountUtilization.organizationName
        accountUtilization.signFlag = updateInvoiceRequest.signFlag ?: accountUtilization.signFlag
        accountUtilization.taxableAmount = updateInvoiceRequest.taxableAmount ?: accountUtilization.taxableAmount
        accountUtilization.zoneCode = updateInvoiceRequest.zoneCode ?: accountUtilization.zoneCode
        accountUtilization.serviceType = updateInvoiceRequest.serviceType.toString() ?: accountUtilization.serviceType
        accountUtilization.category = updateInvoiceRequest.category ?: accountUtilization.category
        accountUtilization.migrated = updateInvoiceRequest.migrated ?: accountUtilization.migrated

        val data = accUtilRepository.update(accountUtilization)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accountUtilization.id,
                actionName = AresConstants.UPDATE,
                data = data,
                performedBy = updateInvoiceRequest.performedBy.toString(),
                performedByUserType = updateInvoiceRequest.performedByType
            )
        )
        Client.addDocument(
            AresConstants.ACCOUNT_UTILIZATION_INDEX,
            accountUtilization.id.toString(),
            accountUtilization
        )
        val accUtilizationRequest = accountUtilizationConverter.convertToModel(accountUtilization)
        try {
            emitDashboardAndOutstandingEvent(accUtilizationRequest)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        // emitAccUtilizationToDemeter(accUtilizationRequest)
    }

    /**
     *
     * @param : updateInvoiceStatusRequest
     */
    override suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest) {
        val accountUtilization = accUtilRepository.findRecord(
            updateInvoiceStatusRequest.oldDocumentNo,
            updateInvoiceStatusRequest.accType.name
        ) ?: throw AresException(AresError.ERR_1005, updateInvoiceStatusRequest.oldDocumentNo.toString())

        var proformaDate: Date? = null
        val proformaQuarter =
            accountUtilization.transactionDate!!.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .get(IsoFields.QUARTER_OF_YEAR)

        val invoiceQuarter =
            updateInvoiceStatusRequest.transactionDate!!.toInstant().atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().get(IsoFields.QUARTER_OF_YEAR)

        if (proformaQuarter != invoiceQuarter) {
            proformaDate = accountUtilization.transactionDate
        }

//         if (accountUtilization.documentStatus == DocumentStatus.FINAL) {
//             throw AresException(AresError.ERR_1202, updateInvoiceStatusRequest.oldDocumentNo.toString())
//         }

        accountUtilization.documentNo = updateInvoiceStatusRequest.newDocumentNo
        accountUtilization.documentValue = updateInvoiceStatusRequest.newDocumentValue
        accountUtilization.documentStatus = updateInvoiceStatusRequest.docStatus
        accountUtilization.updatedAt = Timestamp.from(Instant.now())

        if (updateInvoiceStatusRequest.transactionDate != null) {
            accountUtilization.transactionDate = updateInvoiceStatusRequest.transactionDate
        }

        if (updateInvoiceStatusRequest.dueDate != null) {
            accountUtilization.dueDate = updateInvoiceStatusRequest.dueDate
        }

        accUtilRepository.update(accountUtilization)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accountUtilization.id,
                actionName = AresConstants.UPDATE,
                data = accountUtilization,
                performedBy = updateInvoiceStatusRequest.performedBy.toString(),
                performedByUserType = updateInvoiceStatusRequest.performedByUserType
            )
        )
        val accUtilizationRequest = accountUtilizationConverter.convertToModel(accountUtilization)
        try {
            Client.addDocument(
                AresConstants.ACCOUNT_UTILIZATION_INDEX,
                accountUtilization.id.toString(),
                accountUtilization
            )
            emitDashboardAndOutstandingEvent(accUtilizationRequest, proformaDate)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        // emitAccUtilizationToDemeter(accUtilizationRequest)
    }

    /**
     * Returns Balance Amount and Payment Status for an Invoice
     * @param invoiceRequest
     */
    override suspend fun getInvoicePaymentStatus(invoiceRequest: InvoicePaymentRequest): InvoicePaymentResponse? {
        val accountUtilization = accUtilRepository.findRecord(
            invoiceRequest.documentNo,
            invoiceRequest.accType.name
        ) ?: return null

        return InvoicePaymentResponse(
            documentNo = invoiceRequest.documentNo,
            accType = invoiceRequest.accType,
            balanceAmount = accountUtilization.amountCurr - accountUtilization.payCurr,
            balanceAmountInLedgerCurrency = accountUtilization.amountLoc - accountUtilization.payLoc,
            paymentStatus = Utilities.getPaymentStatus(accountUtilization)
        )
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardAndOutstandingEvent(
        accUtilizationRequest: AccUtilizationRequest,
        proformaDate: Date? = null
    ) {
        if (proformaDate != null) {
            emitDashboardData(accUtilizationRequest, proformaDate)
        }
        emitDashboardData(accUtilizationRequest)
        if (accUtilizationRequest.accMode == AccMode.AR) {
            emitOutstandingData(accUtilizationRequest)
        }
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitOutstandingData(accUtilizationRequest: AccUtilizationRequest) {
        aresKafkaEmitter.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    orgId = accUtilizationRequest.organizationId.toString(),
                    orgName = accUtilizationRequest.organizationName,
                    serviceType = accUtilizationRequest.serviceType,
                    invoiceCurrency = accUtilizationRequest.currency
                )
            )
        )
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardData(accUtilizationRequest: AccUtilizationRequest, proformaDate: Date? = null) {
        val date: Date = proformaDate ?: accUtilizationRequest.transactionDate!!
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(date),
                    quarter = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .get(IsoFields.QUARTER_OF_YEAR),
                    year = date.toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate().year,
                    accMode = accUtilizationRequest.accMode,
                    serviceType = accUtilizationRequest.serviceType,
                    invoiceCurrency = accUtilizationRequest.currency
                )
            )
        )
    }

    private fun getSignFlag(accountType: AccountType): Short {
        for (signSuffix in SignSuffix.values()) {
            if (signSuffix.accountType == accountType) {
                return signSuffix.sign
            }
        }
        throw AresException(AresError.ERR_1205, "accountType")
    }

    private fun emitAccUtilizationToDemeter(accUtilizationRequest: AccUtilizationRequest) {
        try {
            if (accUtilizationRequest.accType == AccountType.PINV)
                aresKafkaEmitter.emitUpdateBillsToArchive(accUtilizationRequest.documentNo)
            else if (accUtilizationRequest.accType == AccountType.SINV)
                aresKafkaEmitter.emitUpdateInvoicesToArchive(accUtilizationRequest.documentNo)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }
    private suspend fun amountGreaterThanExistingRecord(updateInvoiceRequest: UpdateInvoiceRequest, accountUtilization: AccountUtilization, paymentEntry: List<AccountUtilization?>) {
        var extraUtilizationAmountCurr = updateInvoiceRequest.currAmount - accountUtilization.amountCurr
        var extraUtilizationAmountLoc = updateInvoiceRequest.ledAmount - accountUtilization.amountLoc
        for (payment in paymentEntry) {

            if (extraUtilizationAmountCurr <= 0.toBigDecimal() || extraUtilizationAmountLoc <= 0.toBigDecimal())
                continue

            if (payment?.payCurr!! < payment?.amountCurr) {
                var toUpdatePayCurr: BigDecimal
                var toUpdateLedCurr: BigDecimal

                val pendingUtilizationAmount = payment.amountCurr - payment.payCurr
                if (extraUtilizationAmountCurr > pendingUtilizationAmount) {
                    extraUtilizationAmountCurr -= pendingUtilizationAmount
                    toUpdatePayCurr = payment.amountCurr
                } else {
                    toUpdatePayCurr = payment.payCurr.plus(extraUtilizationAmountCurr)
                    extraUtilizationAmountCurr = 0.toBigDecimal()
                }

                val pendingUtilizationAmountLoc = payment.amountLoc - payment.payLoc
                if (extraUtilizationAmountLoc > pendingUtilizationAmountLoc) {
                    extraUtilizationAmountLoc -= pendingUtilizationAmountLoc
                    toUpdateLedCurr = payment.amountLoc
                } else {
                    toUpdateLedCurr = payment.payLoc.plus(extraUtilizationAmountLoc)
                    extraUtilizationAmountLoc = 0.toBigDecimal()
                }

                accUtilRepository.updateAccountUtilization(payment.id!!, toUpdatePayCurr, toUpdateLedCurr)
            }
        }
    }

    private suspend fun amountLessThanExistingRecord(updateInvoiceRequest: UpdateInvoiceRequest, paymentEntry: List<AccountUtilization?>, accountUtilization: AccountUtilization) {
        var totalUtilisedTillNowPay = accountUtilization.payCurr
        var totalUtilisedTillNowLed = accountUtilization.payLoc

        if (updateInvoiceRequest.currAmount >= totalUtilisedTillNowPay && updateInvoiceRequest.ledAmount >= totalUtilisedTillNowLed)
            return

        var extraUtilizationAmountCurr = totalUtilisedTillNowPay - updateInvoiceRequest.currAmount
        var extraUtilizationAmountLoc = totalUtilisedTillNowLed - updateInvoiceRequest.ledAmount
        for (payment in paymentEntry) {
            var toUpdatePayCurr: BigDecimal
            var toUpdateLedCurr: BigDecimal
            if (extraUtilizationAmountCurr > 0.toBigDecimal() && extraUtilizationAmountLoc > 0.toBigDecimal()) {
                if (extraUtilizationAmountCurr > payment?.payCurr && extraUtilizationAmountLoc > payment?.payLoc) {
                    toUpdatePayCurr = 0.toBigDecimal()
                    toUpdateLedCurr = 0.toBigDecimal()
                    extraUtilizationAmountCurr -= payment?.payCurr!!
                    extraUtilizationAmountLoc -= payment.payLoc
                } else {
                    toUpdatePayCurr = payment?.payCurr?.minus(extraUtilizationAmountCurr)!!
                    toUpdateLedCurr = payment?.payLoc?.minus(extraUtilizationAmountLoc)
                    extraUtilizationAmountCurr = 0.toBigDecimal()
                    extraUtilizationAmountLoc = 0.toBigDecimal()
                }

                accUtilRepository.updateAccountUtilization(payment.id!!, toUpdatePayCurr, toUpdateLedCurr)
            }
        }
    }
}
