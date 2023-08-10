package com.cogoport

import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class OutstandingHelper {
    fun getCustomerOutstandingDocument(): CustomerOutstandingDocumentResponse {
        return CustomerOutstandingDocumentResponse(
            organizationId = "9b92503b-6374-4274-9be4-e83a42fc35fe",
            tradePartyId = UUID.randomUUID().toString(),
            businessName = "",
            companyType = "",
            countryCode = "IN",
            countryId = "",
            creditController = null,
            creditDays = "30",
            creditNote = null,
            creditNoteAgeingBucket = null,
            entityCode = 301,
            kam = null,
            onAccount = null,
            onAccountAgeingBucket = null,
            onAccountCount = 0,
            openInvoice = null,
            openInvoiceAgeingBucket = null,
            openInvoiceCount = 0,
            organizationSerialId = "",
            registrationNumber = "AADCS3124K",
            sageId = "52699",
            salesAgent = null,
            totalOutstanding = null,
            tradePartyName = "",
            tradePartySerialId = "",
            tradePartyType = null
        )
    }

    fun getSupplierOutstandingDocument(): SupplierOutstandingDocument {
        return SupplierOutstandingDocument(
            organizationId = "9b92503b-6374-4274-9be4-e83a42fc35fe",
            businessName = "",
            companyType = "",
            countryCode = "IN",
            countryId = "",
            creditDays = "30",
            openInvoice = null,
            openInvoiceCount = 0,
            organizationSerialId = "",
            registrationNumber = "AADCS3124K",
            sageId = "52699",
            totalOutstanding = null,
            category = null,
            collectionPartyType = null,
            creditNoteCount = null,
            ninetyAmount = null,
            ninetyCount = null,
            notDueAmount = null,
            notDueCount = null,
            onAccountPayment = null,
            onAccountPaymentInvoiceCount = null,
            onAccountPaymentInvoiceLedgerAmount = null,
            oneEightyAmount = null,
            oneEightyCount = null,
            selfOrganizationName = "",
            selfOrganizationId = "",
            openInvoiceLedgerAmount = null,
            serialId = "",
            sixtyAmount = null,
            sixtyCount = null,
            supplyAgent = null,
            thirtyAmount = null,
            thirtyCount = null,
            threeSixtyFiveAmount = null,
            threeSixtyFiveCount = null,
            threeSixtyFivePlusAmount = null,
            todayAmount = null,
            todayCount = null,
            threeSixtyFivePlusCount = null,
            totalCreditNoteAmount = null,
            totalOutstandingInvoiceCount = null,
            totalOutstandingInvoiceLedgerAmount = null,
            updatedAt = null,
        )
    }
}
