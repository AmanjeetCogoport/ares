package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate

@Singleton
class InvoiceUtilizationImpl : InvoiceService {

    @Inject
    lateinit var accUtilRepository: AccountUtilizationRepository

    override suspend fun addInvoice(invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse> {

        var responseList = mutableListOf<CreateInvoiceResponse>()

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

            val acUtilization = AccountUtilization(
                null,
                invoiceItem.entityCode,
                invoiceItem.documentNo,
                invoiceItem.orgSerialId,
                invoiceItem.organizationId!!,
                invoiceItem.organizationName,
                invoiceItem.sageOrganizationId,
                invoiceItem.accCode,
                AccountType.valueOf(invoiceItem.accType),
                AccMode.valueOf(invoiceItem.accMode),
                invoiceItem.signFlag,
                invoiceItem.currencyAmount,
                invoiceItem.ledgerAmount,
                invoiceItem.currencyPayment,
                invoiceItem.ledgerPayment,
                LocalDate.parse(invoiceItem.dueDate),
                LocalDate.parse(invoiceItem.transactionDate),
                null,
                null,
                invoiceItem.zoneCode,
                invoiceItem.docStatus,
                invoiceItem.docValue
            )

            val generatedId = accUtilRepository.save(acUtilization).id
            responseList.add(CreateInvoiceResponse(invoiceItem.documentNo, true, Messages.SUCCESS_INVOICE_CREATION))
        }
        return responseList
    }

    override suspend fun addInvoice(invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
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

        val acUtilization = AccountUtilization(
            null,
            invoiceRequest.entityCode,
            invoiceRequest.documentNo,
            invoiceRequest.orgSerialId,
            invoiceRequest.organizationId!!,
            invoiceRequest.organizationName,
            invoiceRequest.sageOrganizationId,
            invoiceRequest.accCode,
            AccountType.valueOf(invoiceRequest.accType),
            AccMode.valueOf(invoiceRequest.accMode),
            invoiceRequest.signFlag,
            invoiceRequest.currencyAmount,
            invoiceRequest.ledgerAmount,
            invoiceRequest.currencyPayment,
            invoiceRequest.ledgerPayment,
            LocalDate.parse(invoiceRequest.dueDate),
            LocalDate.parse(invoiceRequest.transactionDate),
            null,
            null,
            invoiceRequest.zoneCode,
            invoiceRequest.docStatus,
            invoiceRequest.docValue
        )

        accUtilRepository.save(acUtilization)

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
