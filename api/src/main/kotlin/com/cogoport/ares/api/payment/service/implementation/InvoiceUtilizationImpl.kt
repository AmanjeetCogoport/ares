package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class InvoiceUtilizationImpl : InvoiceService {

    @Inject
    lateinit var accUtilRepository: AccountUtilizationRepository

    @Inject
    lateinit var accountUtilizationConverter: AccountUtilizationMapper

    @Inject
    lateinit var emitter: AresKafkaEmitter

    override suspend fun addInvoice(invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse> {

        val responseList = mutableListOf<CreateInvoiceResponse>()

        for (invoiceItem in invoiceRequestList) {

            if (accUtilRepository.isDocumentNumberExists(invoiceItem.documentNo, invoiceItem.accType)) {
                responseList.add(CreateInvoiceResponse(invoiceItem.documentNo, false, AresError.ERR_1201.message))
                continue
            }

            // TODO: More validations to come
            if (!invoiceItem.accType.equals(AccountType.SINV.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PCN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PDN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PINV.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.SCN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.SDN.name, ignoreCase = true)
            ) {
                throw AresException(AresError.ERR_1202, "accType")
            }
            val acUtilization = accountUtilizationConverter.convertToEntity(invoiceItem)
            accUtilRepository.save(acUtilization)
            emitter.emitDashboardData(AccountUtilizationEvent(invoiceItem))
            responseList.add(CreateInvoiceResponse(invoiceItem.documentNo, true, Messages.SUCCESS_INVOICE_CREATION))
        }
        return responseList
    }

    override suspend fun accountUtilization(invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
        if (accUtilRepository.isDocumentNumberExists(invoiceRequest.documentNo, invoiceRequest.accType)) {
            return CreateInvoiceResponse(invoiceRequest.documentNo, false, AresError.ERR_1201.message)
        }
        if (!invoiceRequest.accType.equals(AccountType.SINV.name, ignoreCase = true) &&
            !invoiceRequest.accType.equals(AccountType.PCN.name, ignoreCase = true) &&
            !invoiceRequest.accType.equals(AccountType.PDN.name, ignoreCase = true) &&
            !invoiceRequest.accType.equals(AccountType.PINV.name, ignoreCase = true) &&
            !invoiceRequest.accType.equals(AccountType.SCN.name, ignoreCase = true) &&
            !invoiceRequest.accType.equals(AccountType.SDN.name, ignoreCase = true)
        ) {
            throw AresException(AresError.ERR_1202, "accType")
        }
        val acUtilization = accountUtilizationConverter.convertToEntity(invoiceRequest)
        accUtilRepository.save(acUtilization)
        emitter.emitDashboardData(AccountUtilizationEvent(invoiceRequest))
        return CreateInvoiceResponse(invoiceRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION)
    }

    override suspend fun deleteInvoice(docId: Long, accType: String): Boolean {
        // TODO : validations before deletetion
        accUtilRepository.deleteInvoiceUtils(docId, accType)
        return true
    }

    override suspend fun findByDocumentNo(docNumber: Long): AccountUtilization {
        return accUtilRepository.findByDocumentNo(docNumber)
    }
}
