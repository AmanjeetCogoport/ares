package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.event.CreateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.kafka.common.KafkaException
import java.sql.SQLException
import java.sql.Timestamp
import java.text.DateFormat
import java.time.Instant
import javax.transaction.Transactional

@Singleton
open class InvoiceUtilizationImpl : InvoiceService {

    @Inject
    lateinit var accUtilRepository: AccountUtilizationRepository

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var dateFormatter: DateFormat

    @Inject
    lateinit var accountUtilizationConverter: AccountUtilizationMapper

    /**
     * @param accUtilizationRequestList
     * @return mutableListOf CreateInvoiceResponse
     */
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [KafkaException::class])
    override suspend fun addInvoice(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {

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

            acUtilization.accCode = AresModelConstants.AP_ACCOUNT_CODE
            if (accUtilizationRequest.accMode == AccMode.AR) {
                acUtilization.accCode = AresModelConstants.AR_ACCOUNT_CODE
            }

            val generatedId = accUtilRepository.save(acUtilization).id!!

            try {
                emitDashboardEvent(accUtilizationRequest)
            } catch (k: KafkaException) {
                logger().error(k.stackTraceToString())
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
            }
            responseList.add(CreateInvoiceResponse(generatedId!!, accUtilizationRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION))
        }
        return responseList
    }

    /**
     * Add account utilization
     * @param accUtilizationRequest
     * @return CreateInvoiceResponse
     */
    override suspend fun addAccountUtilization(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse {

        if (!Utilities.isInvoiceAccountType(accUtilizationRequest.accType!!)) {
            return CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1202.message)
        }
        if (accUtilRepository.isDocumentNumberExists(accUtilizationRequest.documentNo, accUtilizationRequest.accType!!.name)) {
            return CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1201.message)
        }
        val acUtilization = accountUtilizationConverter.convertToEntity(accUtilizationRequest)
        acUtilization.createdAt = Timestamp.from(Instant.now())
        acUtilization.updatedAt = Timestamp.from(Instant.now())

        acUtilization.accCode = AresModelConstants.AP_ACCOUNT_CODE
        if (accUtilizationRequest.accMode == AccMode.AR) {
            acUtilization.accCode = AresModelConstants.AR_ACCOUNT_CODE
        }

        val generatedId = accUtilRepository.save(acUtilization).id

        try {
            emitDashboardEvent(accUtilizationRequest)
        } catch (k: KafkaException) {
            logger().error(k.stackTraceToString())
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        return CreateInvoiceResponse(generatedId!!, accUtilizationRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION)
    }

    /**
     *
     */
    override suspend fun deleteInvoice(data: MutableList<Pair<Long, String>>): Boolean {

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

            result = true
        }
        return result
    }

    override suspend fun findByDocumentNo(docNumber: Long): AccountUtilization {
        return accUtilRepository.findByDocumentNo(docNumber)
    }

    /**
     * Updates Invoice in account utilization
     * @param UpdateInvoiceRequest
     */
    @Transactional
    override suspend fun updateInvoice(accUtilizationRequest: AccUtilizationRequest) {
        deleteInvoice(accUtilizationRequest.documentNo, accUtilizationRequest.accType!!.name)
        addAccountUtilization(accUtilizationRequest)
    }

    override suspend fun updateInvoiceStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest) {
        TODO("Not yet implemented")
    }

    /**
     * Updates Invoice in account utilization
     * @param UpdateInvoiceRequest
     */
    @Transactional
    override suspend fun deleteCreateInvoice(createInvoiceRequest: CreateInvoiceRequest) {
        TODO()
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardEvent(accUtilizationRequest: AccUtilizationRequest) {
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = dateFormatter.format(accUtilizationRequest.dueDate),
                    quarter = (accUtilizationRequest.dueDate!!.month / 3) + 1,
                    year = accUtilizationRequest.dueDate!!.year
                )
            )
        )
    }
}
