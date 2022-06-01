package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.text.DateFormat
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
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    override suspend fun addInvoice(accUtilizationRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse> {
        val responseList = mutableListOf<CreateInvoiceResponse>()
        for (accUtilizationRequest in accUtilizationRequestList) {
            if (accUtilRepository.isDocumentNumberExists(accUtilizationRequest.documentNo, accUtilizationRequest.accType.name)) {
                responseList.add(CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1201.message))
                continue
            }
            val acUtilization = accountUtilizationConverter.convertToEntity(accUtilizationRequest)
            val generatedId = accUtilRepository.save(acUtilization).id
            emitDashboardEvent(accUtilizationRequest)
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
        if (accUtilRepository.isDocumentNumberExists(accUtilizationRequest.documentNo, accUtilizationRequest.accType.name)) {
            return CreateInvoiceResponse(0L, accUtilizationRequest.documentNo, false, AresError.ERR_1201.message)
        }
        val acUtilization = accountUtilizationConverter.convertToEntity(accUtilizationRequest)
        val generatedId = accUtilRepository.save(acUtilization).id
        emitDashboardEvent(accUtilizationRequest)
        return CreateInvoiceResponse(generatedId!!, accUtilizationRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION)
    }

    /**
     *
     */
    override suspend fun deleteInvoice(docNumber: Long, accType: String): Boolean {
        accUtilRepository.deleteInvoiceUtils(docNumber, accType)
        return true
    }

    override suspend fun findByDocumentNo(docNumber: Long): AccountUtilization {
        return accUtilRepository.findByDocumentNo(docNumber)
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * @param accUtilizationRequest
     */
    private fun emitDashboardEvent(accUtilizationRequest: AccUtilizationRequest){
        aresKafkaEmitter.emitDashboardData(OpenSearchEvent(OpenSearchRequest(
            zone = accUtilizationRequest.zoneCode,
            date = dateFormatter.format(accUtilizationRequest.dueDate),
            quarter = (accUtilizationRequest.dueDate!!.month/3)+1,
            year = accUtilizationRequest.dueDate!!.year
        )))
    }
}
