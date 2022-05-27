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
import java.sql.Timestamp
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
                entityCode = invoiceItem.entityCode,
                documentNo = invoiceItem.documentNo,
                orgSerialId = invoiceItem.orgSerialId,
                organizationId = invoiceItem.organizationId!!,
                organizationName = invoiceItem.organizationName,
                sageOrganizationId = invoiceItem.sageOrganizationId,
                accCode = invoiceItem.accCode,
                accType = AccountType.valueOf(invoiceItem.accType),
                accMode = AccMode.valueOf(invoiceItem.accMode),
                signFlag = invoiceItem.signFlag,
                amountCurr = invoiceItem.currencyAmount,
                amountLoc = invoiceItem.ledgerAmount,
                payCurr = invoiceItem.currencyPayment,
                payLoc = invoiceItem.ledgerPayment,
                dueDate = Timestamp.valueOf(invoiceItem.dueDate),
                transactionDate = Timestamp.valueOf(invoiceItem.transactionDate),
                zoneCode = invoiceItem.zoneCode,
                docStatus = invoiceItem.docStatus,
                documentValue = invoiceItem.docValue
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
            entityCode = invoiceRequest.entityCode,
            documentNo = invoiceRequest.documentNo,
            orgSerialId = invoiceRequest.orgSerialId,
            organizationId = invoiceRequest.organizationId!!,
            organizationName = invoiceRequest.organizationName,
            sageOrganizationId = invoiceRequest.sageOrganizationId,
            accCode = invoiceRequest.accCode,
            accType = AccountType.valueOf(invoiceRequest.accType),
            accMode = AccMode.valueOf(invoiceRequest.accMode),
            signFlag = invoiceRequest.signFlag,
            amountCurr = invoiceRequest.currencyAmount,
            amountLoc = invoiceRequest.ledgerAmount,
            payCurr = invoiceRequest.currencyPayment,
            payLoc = invoiceRequest.ledgerPayment,
            dueDate = Timestamp.valueOf(invoiceRequest.dueDate),
            transactionDate = Timestamp.valueOf(invoiceRequest.transactionDate),
            zoneCode = invoiceRequest.zoneCode,
            docStatus = invoiceRequest.docStatus,
            documentValue = invoiceRequest.docValue
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
