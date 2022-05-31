package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.transaction.Transactional

@Singleton
open class InvoiceUtilizationImpl : InvoiceService {

    @Inject
    lateinit var accUtilRepository: AccountUtilizationRepository

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    override suspend fun addInvoice(invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse> {

        val responseList = mutableListOf<CreateInvoiceResponse>()

        for (invoiceItem in invoiceRequestList) {
            var accType: AccountType
            try {
                accType = AccountType.valueOf(invoiceItem.accType)
            }catch (ex:IllegalArgumentException){
                throw AresException(AresError.ERR_1202," accType")
            }

            if (accUtilRepository.isDocumentNumberExists(invoiceItem.documentNo, accType.name)) {
                responseList.add(CreateInvoiceResponse(0L, invoiceItem.documentNo, false, AresError.ERR_1201.message))
                continue
            }

            if (!invoiceItem.accType.equals(AccountType.SINV.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PCN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PDN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.PINV.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.SCN.name, ignoreCase = true) &&
                !invoiceItem.accType.equals(AccountType.SDN.name, ignoreCase = true)
            ) {
                throw AresException(AresError.ERR_1202, " accType")
            }
            val dueDate = Utilities.getTimeStampFromString(invoiceItem.dueDate!!)
            val transactionDate = Utilities.getTimeStampFromString(invoiceItem.transactionDate!!)

            val acUtilization = AccountUtilization(
                null,
                invoiceItem.documentNo,
                invoiceItem.docValue,
                invoiceItem.zoneCode,
                invoiceItem.serviceType!!,
                DocumentStatus.valueOf(invoiceItem.docStatus),
                invoiceItem.entityCode,
                invoiceItem.category!!,
                invoiceItem.orgSerialId,
                invoiceItem.sageOrganizationId,
                invoiceItem.organizationId!!,
                invoiceItem.organizationName,
                invoiceItem.accCode,
                accType,
                AccMode.valueOf(invoiceItem.accMode),
                invoiceItem.signFlag,
                invoiceItem.currency,
                invoiceItem.ledCurrency,
                invoiceItem.currencyAmount,
                invoiceItem.ledgerAmount,
                invoiceItem.currencyPayment,
                invoiceItem.ledgerPayment,
                dueDate,
                transactionDate,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
            )
            val generatedId = accUtilRepository.save(acUtilization).id
            responseList.add(CreateInvoiceResponse(generatedId!!, invoiceItem.documentNo, true, Messages.SUCCESS_INVOICE_CREATION))
        }
        return responseList
    }

    override suspend fun addInvoice(invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {

        val accType: AccountType
        try {
            accType = AccountType.valueOf(invoiceRequest.accType)
        }catch (ex:IllegalArgumentException){
            throw AresException(AresError.ERR_1202," accType")
        }

        if (accUtilRepository.isDocumentNumberExists(invoiceRequest.documentNo, accType.name)) {
            return CreateInvoiceResponse(0L, invoiceRequest.documentNo, false, AresError.ERR_1201.message)
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

        val dueDate = Utilities.getTimeStampFromString(invoiceRequest.dueDate!!)
        val transactionDate = Utilities.getTimeStampFromString(invoiceRequest.transactionDate!!)

        val acUtilization = AccountUtilization(
            null,
            invoiceRequest.documentNo,
            invoiceRequest.docValue,
            invoiceRequest.zoneCode,
            invoiceRequest.serviceType!!,
            DocumentStatus.valueOf(invoiceRequest.docStatus),
            invoiceRequest.entityCode,
            invoiceRequest.category!!,
            invoiceRequest.orgSerialId,
            invoiceRequest.sageOrganizationId,
            invoiceRequest.organizationId!!,
            invoiceRequest.organizationName,
            invoiceRequest.accCode,
            accType,
            AccMode.valueOf(invoiceRequest.accMode),
            invoiceRequest.signFlag,
            invoiceRequest.currency,
            invoiceRequest.ledCurrency,
            invoiceRequest.currencyAmount,
            invoiceRequest.ledgerAmount,
            invoiceRequest.currencyPayment,
            invoiceRequest.ledgerPayment,
            dueDate,
            transactionDate,
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now())
        )
        val generatedId = accUtilRepository.save(acUtilization).id
        return CreateInvoiceResponse(generatedId!!, invoiceRequest.documentNo, true, Messages.SUCCESS_INVOICE_CREATION)
    }

    override suspend fun deleteInvoice(docNumber: Long, accType: String): Boolean {
        accUtilRepository.deleteInvoiceUtils(docNumber, accType)
        return true
    }

    override suspend fun findByDocumentNo(docNumber: Long): AccountUtilization {
        return accUtilRepository.findByDocumentNo(docNumber)
    }
}
