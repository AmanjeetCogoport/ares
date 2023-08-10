package com.cogoport

import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class OutstandingHelper {
    fun getSupplierDetailObject(): CustomerOutstandingDocumentResponse {
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
}
