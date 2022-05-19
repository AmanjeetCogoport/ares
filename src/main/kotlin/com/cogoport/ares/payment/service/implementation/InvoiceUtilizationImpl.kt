package com.cogoport.ares.payment.service.implementation

import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.payment.entity.AccountUtilization
import com.cogoport.ares.payment.model.AccUtilizationRequest
import com.cogoport.ares.payment.model.CreateInvoiceResponse
import com.cogoport.ares.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.payment.service.interfaces.InvoiceService
import com.cogoport.ares.utils.code.AresError
import com.cogoport.ares.utils.exception.AresException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate

@Singleton
class InvoiceUtilizationImpl:InvoiceService {

    @Inject
    lateinit var accUtilRepository:AccountUtilizationRepository

    override suspend fun addInvoice(invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {

        if(accUtilRepository.isDocumentNumberExists(invoiceRequest.documentNo,invoiceRequest.accType)){
            throw AresException(AresError.ERR_1201,"documentNo")
        }

        //TODO: More validations to come
        val acUtilization=AccountUtilization(null,invoiceRequest.documentNo,invoiceRequest.entityCode,invoiceRequest.entityId,
        invoiceRequest.orgSerialId,invoiceRequest.sageOrganizationId,invoiceRequest.organizationId,invoiceRequest.organizationName,
        invoiceRequest.accCode,invoiceRequest.accType,invoiceRequest.accMode,invoiceRequest.signFlag,invoiceRequest.currencyAmount,
        invoiceRequest.ledgerAmount,invoiceRequest.currencyPayment,invoiceRequest.ledgerPayment,LocalDate.parse(invoiceRequest.dueDate),
        LocalDate.parse(invoiceRequest.transactionDate),null,null,invoiceRequest.zoneCode,invoiceRequest.docStatus,invoiceRequest.docValue)

       val generatedId = accUtilRepository.save(acUtilization).id

        return CreateInvoiceResponse(generatedId!!,Messages.SUCESS_INVOICE_CREATION)
    }


    override suspend fun deleteInvoice(docId: Long, accType: String): Boolean {
        //TODO : validations before deletetion
        accUtilRepository.deleteInvoiceUtils(docId, accType)
        return true
    }
}