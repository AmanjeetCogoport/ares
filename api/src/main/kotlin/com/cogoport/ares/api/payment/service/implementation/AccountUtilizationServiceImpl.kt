package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.CogoClient
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.kafka.common.KafkaException
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
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
    lateinit var cogoClient: CogoClient

    /**
     * @param accUtilizationRequestList
     * @return listOf CreateInvoiceResponse
     */
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {

        val responseList = mutableListOf<CreateInvoiceResponse>()
        for (accUtilizationRequest in accUtilizationRequestList) {

            if (!Utilities.isInvoiceAccountType(accUtilizationRequest.accType!!)) {
                responseList.add(CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1202.message))
                continue
            }
            if (accUtilRepository.isDocumentNumberExists(accUtilizationRequest.documentNo, accUtilizationRequest.accType!!.name)) {
                responseList.add(CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1201.message))
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

            try {
                emitDashboardAndOutstandingEvent(accUtilizationRequest)
                Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            } catch (k: KafkaException) {
                logger().error(k.stackTraceToString())
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
            }
            responseList.add(CreateInvoiceResponse(accUtilRes.id!!, accUtilizationRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION))
        }
        return responseList
    }

    /**
     * Add account utilization
     * @param accUtilizationRequest
     * @return CreateInvoiceResponse
     */
    override suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse {
        var accUtilizationList = mutableListOf<AccUtilizationRequest>()
        accUtilizationList.add(accUtilizationRequest)
        val listResponse = add(accUtilizationList)

        return listResponse[0]
    }

    override suspend fun delete(data: MutableList<Pair<Long, String>>): Boolean {
        var result = false
        for (obj in data) {

            var accountUtilization = accUtilRepository.findRecord(obj.first, obj.second)

            if (accountUtilization == null) {
                throw AresException(AresError.ERR_1005, obj.first.toString())
            }
            if (Utilities.isPayAccountType(accountUtilization.accType)) {
                throw AresException(AresError.ERR_1202, obj.first.toString())
            }
            if (accountUtilization.documentStatus == DocumentStatus.FINAL) {
                throw AresException(AresError.ERR_1204, obj.first.toString())
            }
            accUtilRepository.deleteInvoiceUtils(accountUtilization.id!!)

            var accUtilizationRequest = accountUtilizationConverter.convertToModel(accountUtilization)

            emitDashboardAndOutstandingEvent(accUtilizationRequest)

            try {
                Client.removeDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accountUtilization.id.toString())
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
            }
            result = true
        }
        return result
    }

    /**
     * Updates Invoice in account utilization
     * @param UpdateInvoiceRequest
     */
    override suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest) {

        var accountUtilization = accUtilRepository.findRecord(updateInvoiceRequest.documentNo, updateInvoiceRequest.accType.name)

        if (accountUtilization == null) {
            throw AresException(AresError.ERR_1005, updateInvoiceRequest.documentNo.toString())
        }

        accountUtilization.transactionDate = updateInvoiceRequest.transactionDate
        accountUtilization.dueDate = updateInvoiceRequest.dueDate
        accountUtilization.documentStatus = updateInvoiceRequest.docStatus
        accountUtilization.entityCode = updateInvoiceRequest.entityCode
        accountUtilization.currency = updateInvoiceRequest.currency
        accountUtilization.ledCurrency = updateInvoiceRequest.ledCurrency
        accountUtilization.amountCurr = updateInvoiceRequest.currAmount
        accountUtilization.amountLoc = updateInvoiceRequest.ledAmount
        accountUtilization.updatedAt = Timestamp.from(Instant.now())

        accUtilRepository.update(accountUtilization)

        var accUtilizationRequest = accountUtilizationConverter.convertToModel(accountUtilization)
        emitDashboardAndOutstandingEvent(accUtilizationRequest)
        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accountUtilization!!.id.toString(), accountUtilization)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    /**
     *
     * @param : updateInvoiceStatusRequest
     */
    override suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest) {
        var accountUtilization = accUtilRepository.findRecord(updateInvoiceStatusRequest.oldDocumentNo, updateInvoiceStatusRequest.accType.name)

        if (accountUtilization == null) {
            throw AresException(AresError.ERR_1005, updateInvoiceStatusRequest.oldDocumentNo.toString())
        }
        if (accountUtilization.documentStatus == DocumentStatus.FINAL) {
            throw AresException(AresError.ERR_1202, updateInvoiceStatusRequest.oldDocumentNo.toString())
        }

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

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accountUtilization!!.id.toString(), accountUtilization)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardAndOutstandingEvent(accUtilizationRequest: AccUtilizationRequest) {
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
                    orgName = accUtilizationRequest.organizationName
                )
            )
        )
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardData(accUtilizationRequest: AccUtilizationRequest) {
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(accUtilizationRequest.transactionDate),
                    quarter = accUtilizationRequest.transactionDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .get(IsoFields.QUARTER_OF_YEAR),
                    year = accUtilizationRequest.transactionDate!!.toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate().year,
                    accMode = accUtilizationRequest.accMode
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
}
