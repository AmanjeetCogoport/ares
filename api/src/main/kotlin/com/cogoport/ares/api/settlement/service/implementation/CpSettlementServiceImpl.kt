package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.BankDetails
import com.cogoport.ares.api.common.models.CogoBanksDetails
import com.cogoport.ares.api.common.models.TdsStylesResponse
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.OrgIdAndEntityCode
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.InvoiceDocumentMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.CpSettlementService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.InvoiceDocumentResponse
import com.cogoport.ares.model.settlement.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.plutus.client.PlutusClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil

@Singleton
open class CpSettlementServiceImpl : CpSettlementService {

    @Inject
    lateinit var documentConverter: DocumentMapper

    @Inject
    lateinit var plutusClient: PlutusClient

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var invoiceDocumentConverter: InvoiceDocumentMapper

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var cogoClient: AuthClient

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    /**
     * Get invoices for Given CP orgId.
     * @param settlementInvoiceRequest
     * @return ResponseList
     */
    override suspend fun getInvoices(settlementInvoiceRequest: SettlementInvoiceRequest) = getInvoiceList(settlementInvoiceRequest)

    /**
     * *
     * - add entry into payments table
     * - add into account utilization table
     * - add entries into settlement table
     * - invoice knocked off with amount
     * - tds entry
     * - payment entry
     * - convenience fee entry [like EXC/TDS] for accounting of payment
     * - update utilization table with balance or status
     */
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun knockoff(request: SettlementKnockoffRequest): SettlementKnockoffResponse {
        val cogoEntities = cogoClient.getCogoBank(CogoEntitiesRequest())
        var cogoEntity: CogoBanksDetails? = null
        var selectedBank: BankDetails? = null
        for (bank in cogoEntities.bankList) {
            val banksDetails = bank.bankDetails?.filter { it.accountNumber == request.cogoAccountNo }
            if (banksDetails?.size!! >= 1) {
                cogoEntity = bank
                selectedBank = banksDetails.get(0)
            }
        }

        if (cogoEntity == null || selectedBank == null) {
            throw AresException(AresError.ERR_1002, AresConstants.ZONE)
        }

        logger().info(cogoEntities.toString())

        val invoiceUtilization =
            accountUtilizationRepository.findRecordByDocumentValue(
                documentValue = request.invoiceNumber,
                accType = AccountType.SINV.toString(),
                accMode = AccMode.AR.toString()
            ) ?: throw AresException(AresError.ERR_1002, AresConstants.ZONE)

        val payment = settledInvoiceConverter.convertKnockoffRequestToEntity(request)
        payment.organizationId = invoiceUtilization.organizationId
        payment.organizationName = invoiceUtilization.organizationName
        payment.bankId = selectedBank.id
        payment.entityCode = cogoEntity.entityCode
        payment.bankName = selectedBank.beneficiaryName

        payment.exchangeRate =
            settlementServiceHelper.getExchangeRate(
                payment.currency,
                invoiceUtilization.ledCurrency,
                SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
            )
        payment.ledCurrency = invoiceUtilization.ledCurrency
        payment.accMode = AccMode.AR
        payment.ledAmount = payment.amount * payment.exchangeRate!!

        // Utilization of payment
        val documentNo = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)

        val accountUtilization =
            AccountUtilization(
                id = null,
                documentNo = documentNo,
                documentValue = request.transactionId,
                zoneCode = invoiceUtilization.zoneCode,
                serviceType = invoiceUtilization.serviceType,
                documentStatus = DocumentStatus.FINAL,
                entityCode = invoiceUtilization.entityCode,
                category = invoiceUtilization.category,
                orgSerialId = invoiceUtilization.orgSerialId,
                sageOrganizationId = invoiceUtilization.sageOrganizationId,
                organizationId = invoiceUtilization.organizationId,
                taggedOrganizationId = invoiceUtilization.taggedOrganizationId,
                tradePartyMappingId = invoiceUtilization.tradePartyMappingId,
                organizationName = invoiceUtilization.organizationName,
                accType = AccountType.REC,
                accCode = invoiceUtilization.accCode,
                signFlag = 1,
                currency = request.currency,
                ledCurrency = invoiceUtilization.ledCurrency,
                amountCurr = request.amount,
                amountLoc = request.amount,
                taxableAmount = BigDecimal.ZERO,
                payCurr = BigDecimal.ZERO,
                payLoc = BigDecimal.ZERO,
                accMode = invoiceUtilization.accMode,
                transactionDate = request.transactionDate,
                dueDate = request.transactionDate,
                migrated = false,
                settlementEnabled = true,
                isProforma = false
            )

        val isTdsApplied =
            settlementRepository.countDestinationBySourceType(
                invoiceUtilization.documentNo,
                SettlementType.SINV,
                SettlementType.CTDS
            ) > 0

        val settlements = mutableListOf<Settlement>()
        val amountToSettle = invoiceUtilization.amountCurr - invoiceUtilization.payCurr
        var payAmount = request.amount
        if (invoiceUtilization.currency != request.currency) {
            payAmount =
                payment.amount *
                settlementServiceHelper.getExchangeRate(
                    request.currency,
                    invoiceUtilization.currency,
                    SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
                )
        }

        val settledAmount: BigDecimal = if (payAmount > amountToSettle) {
            amountToSettle
        } else {
            payAmount
        }

        settlements.add(
            Settlement(
                id = null,
                sourceId = accountUtilization.documentNo,
                sourceType = SettlementType.REC,
                destinationId = invoiceUtilization.documentNo,
                destinationType = SettlementType.SINV,
                currency = invoiceUtilization.currency,
                amount = settledAmount,
                ledCurrency = invoiceUtilization.ledCurrency,
                ledAmount =
                settledAmount *
                    payment.exchangeRate!!,
                signFlag = 1,
                settlementDate = Timestamp.from(Instant.now()),
                createdAt = Timestamp.from(Instant.now()),
                createdBy = null,
                updatedBy = null,
                updatedAt = Timestamp.from(Instant.now()),
                settlementNum = sequenceGeneratorImpl.getSettlementNumber(),
                settlementStatus = SettlementStatus.CREATED
            )
        )

        if (!isTdsApplied) {
            val tds: BigDecimal = invoiceUtilization.taxableAmount!!.multiply(0.02.toBigDecimal())
            settlements.add(
                Settlement(
                    id = null,
                    sourceId = accountUtilization.documentNo,
                    sourceType = SettlementType.CTDS,
                    destinationId = invoiceUtilization.documentNo,
                    destinationType = SettlementType.SINV,
                    currency = invoiceUtilization.currency,
                    amount = tds,
                    ledCurrency = invoiceUtilization.ledCurrency,
                    ledAmount =
                    tds *
                        settlementServiceHelper.getExchangeRate(
                            invoiceUtilization.ledCurrency,
                            invoiceUtilization.currency,
                            SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(invoiceUtilization.transactionDate)
                        ),
                    signFlag = 1,
                    settlementDate = Timestamp.from(Instant.now()),
                    createdAt = Timestamp.from(Instant.now()),
                    createdBy = null,
                    updatedBy = null,
                    updatedAt = Timestamp.from(Instant.now()),
                    settlementNum = sequenceGeneratorImpl.getSettlementNumber(),
                    settlementStatus = SettlementStatus.CREATED
                )
            )
        }

        val settledAmtLed =
            settledAmount * invoiceUtilization.amountLoc / invoiceUtilization.amountCurr
        val settledAmtFromRec = settledAmount * payment.exchangeRate!!

        if ((settledAmtLed - settledAmtFromRec) != BigDecimal.ZERO) {
            settlements.add(
                Settlement(
                    id = null,
                    sourceId = accountUtilization.documentNo,
                    sourceType = SettlementType.SECH,
                    destinationId = invoiceUtilization.documentNo,
                    destinationType = SettlementType.SINV,
                    currency = null,
                    amount = null,
                    ledCurrency = invoiceUtilization.ledCurrency,
                    ledAmount = settledAmtFromRec - settledAmtLed,
                    signFlag = 1,
                    settlementDate = Timestamp.from(Instant.now()),
                    createdAt = Timestamp.from(Instant.now()),
                    createdBy = null,
                    updatedBy = null,
                    updatedAt = Timestamp.from(Instant.now()),
                    settlementNum = sequenceGeneratorImpl.getSettlementNumber(),
                    settlementStatus = SettlementStatus.CREATED
                )
            )
        }

        invoiceUtilization.payCurr = invoiceUtilization.payCurr + settledAmount
        invoiceUtilization.payLoc = invoiceUtilization.payCurr * payment.exchangeRate!!
        invoiceUtilization.updatedAt = Timestamp.from(Instant.now())

        accountUtilization.payLoc = settledAmount
        accountUtilization.payCurr = accountUtilization.payCurr * payment.exchangeRate!!

        // 2% tds on taxable amount only if tds is not deducted already
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())
        payment.paymentDocumentStatus = payment.paymentDocumentStatus ?: PaymentDocumentStatus.CREATED
        val paymentObj = paymentRepository.save(payment)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PAYMENTS,
                objectId = paymentObj.id,
                actionName = AresConstants.CREATE,
                data = paymentObj,
                performedBy = request.performedBy.toString(),
                performedByUserType = request.performedByUserType
            )
        )

        val invUtilObj = accountUtilizationRepository.update(invoiceUtilization)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = invUtilObj.id,
                actionName = AresConstants.UPDATE,
                data = invUtilObj,
                performedBy = request.performedBy.toString(),
                performedByUserType = request.performedByUserType
            )
        )

        val accUtilObj = accountUtilizationRepository.save(accountUtilization)
        if (accUtilObj.accMode == AccMode.AR) {
            aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilization.organizationId))
            aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountUtilization.organizationId!!, accountUtilization.entityCode))
        }

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilObj.id,
                actionName = AresConstants.CREATE,
                data = accUtilObj,
                performedBy = request.performedBy.toString(),
                performedByUserType = request.performedByUserType
            )
        )
        settlements.forEach { settlement ->
            val settleObj = settlementRepository.save(settlement)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.SETTLEMENT,
                    objectId = settleObj.id,
                    actionName = AresConstants.CREATE,
                    data = settleObj,
                    performedBy = request.performedBy.toString(),
                    performedByUserType = request.performedByUserType
                )
            )
        }
        return SettlementKnockoffResponse()
    }

    /**
     * Get List of invoices for CP.
     * @param request
     * @return ResponseList
     */
    private suspend fun getInvoiceList(request: SettlementInvoiceRequest): ResponseList<SettlementInvoiceResponse> {
        validateInvoiceRequest(request)
        val response = getInvoiceDocumentList(request)
        val invoiceList = documentConverter.convertToSettlementInvoice(response.list)

        val invoiceIds = invoiceList.map { it?.invoiceNo }
        val invoiceSids = if (invoiceIds.isNotEmpty()) plutusClient.getSidsForInvoiceIds(invoiceIds as List<String>) else null

        for (doc in invoiceList) {
            val d = invoiceSids?.find { it.invoiceId == doc?.invoiceNo }
            doc?.sid = d?.jobNumber
            doc?.shipmentType = d?.shipmentType
            doc?.pdfUrl = d?.pdfUrl
        }
        return ResponseList(
            list = invoiceList,
            totalPages = response.totalPages,
            totalRecords = response.totalRecords,
            pageNo = request.page
        )
    }

    private fun validateInvoiceRequest(request: SettlementInvoiceRequest) {
        if (request.orgId.isEmpty()) throw AresException(AresError.ERR_1003, "orgId")
        if (request.status == null) throw AresException(AresError.ERR_1003, "status")
        if (request.accType == null) throw AresException(AresError.ERR_1003, "accType")
        if (request.accType !in listOf(AccountType.SINV, AccountType.PINV)) throw AresException(AresError.ERR_1202, "")
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param request
     * @return ResponseList
     */
    private suspend fun getInvoiceDocumentList(
        request: SettlementInvoiceRequest
    ): ResponseList<InvoiceDocumentResponse> {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        var query: String? = null
        if (request.query != null) {
            query = "%${request.query}%"
        }
        val documentEntity =
            accountUtilizationRepository.getInvoiceDocumentList(
                request.pageLimit,
                offset,
                request.accType,
                request.orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                query,
                request.status.toString()
            )
        val documentModel = documentEntity.map { invoiceDocumentConverter.convertToModel(it!!) }
        val tdsStyles = fetchTdsStyles(request.orgId)
        documentModel.forEach { doc ->
            doc.tdsPercentage = getTdsRate(
                tdsProfile = tdsStyles,
                orgId = doc.organizationId
            )

            doc.tds = doc.taxableAmount.multiply(
                doc.tdsPercentage!!.divide(100.toBigDecimal())
            ).setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN).minus(doc.settledTds)

            doc.afterTdsAmount =
                doc.documentAmount.setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN).minus(doc.tds!!)

            doc.balanceAmount =
                doc.balanceAmount.setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN).minus(doc.tds!!)
        }
        val total =
            accountUtilizationRepository.getInvoiceDocumentCount(
                request.accType,
                request.orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                query,
                request.status.toString()
            )
        for (doc in documentModel) {
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.balanceAmount,
                docType = SettlementType.valueOf(doc.accountType.dbValue)
            )
        }
        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    private suspend fun fetchTdsStyles(orgIds: List<UUID>): MutableList<TdsStylesResponse> {
        val tdsStyles = mutableListOf<TdsStylesResponse>()
        for (orgId in orgIds) {
            try {
                tdsStyles.add(cogoClient.getOrgTdsStyles(orgId.toString()).data)
            } catch (e: Exception) {
                logger().error("Tds Style Not Found for organization id: {}", orgId)
                continue
            }
        }
        return tdsStyles
    }

    private fun getTdsRate(
        tdsProfile: List<TdsStylesResponse>,
        orgId: UUID
    ): BigDecimal {
        val tdsStyle = tdsProfile.find { it.id == orgId }
        return if (tdsStyle?.tdsDeductionType == "no_deduction") {
            AresConstants.NO_DEDUCTION_RATE.toBigDecimal()
        } else {
            tdsStyle?.tdsDeductionRate ?: AresConstants.DEFAULT_TDS_RATE.toBigDecimal()
        }
    }
}
