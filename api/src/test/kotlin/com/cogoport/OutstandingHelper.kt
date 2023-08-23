package com.cogoport

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.response.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.UUID

@Singleton
class OutstandingHelper {
    fun getCustomerOutstandingDocument(): CustomerOutstandingDocumentResponse {
        return CustomerOutstandingDocumentResponse(
            organizationId = "9b92503b-6374-4274-9be4-e83a42fc35fe",
            tradePartyId = "1e3ed3f5-da62-4c81-bbab-7608fdac892d",
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
            tradePartyType = null,
            lastUpdatedAt = Timestamp(1691663743000),
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
            updatedAt = Timestamp(1691663743000),
        )
    }

    fun saveCustomerInvoiceResponseDoc() {
        val response = CustomerInvoiceResponse(
            balanceAmount = 0.toBigDecimal(),
            currency = "INR",
            docType = "SINV",
            invoiceAmount = 400.toBigDecimal(),
            invoiceDate = SimpleDateFormat("yyyy-DD-mm").parse("2022-02-02"),
            invoiceDueDate = SimpleDateFormat("yyyy-DD-mm").parse("2022-02-02"),
            invoiceNumber = "COGO09800",
            invoiceType = "INVOICE",
            organizationId = "9b92503b-6374-4274-9be4-e83a42fc35fe",
            organizationName = "TEST",
            overdueDays = 0,
            shipmentId = 21213,
            shipmentType = "FCL"
        )
        Client.addDocument(AresConstants.INVOICE_OUTSTANDING_INDEX, UUID.randomUUID().toString(), response, true)
    }
}
