package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.DueAmount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@ReflectiveAccess
data class CustomerOutstandingDocumentResponse(
        @JsonProperty("organizationId")
        var organizationId: String?,
        @JsonProperty("tradePartyId")
        var tradePartyId: String?,
        @JsonProperty("businessName")
        var businessName: String?,
        @JsonProperty("tradePartyName")
        var tradePartyName: String?,
        @JsonProperty("legalBusinessName")
        var legalBusinessName: String?,
        @JsonProperty("tradePartyType")
        var tradePartyType: String?,
        @JsonProperty("registrationNumber")
        var registrationNumber: String?,
        @JsonProperty("companyType")
        var companyType: String?,
        @JsonProperty("creditController")
        var creditController: UUID?,
        @JsonProperty("kam")
        var kam: SalesAgent?,
        @JsonProperty("salesAgent")
        var salesAgent: SalesAgent?,
        @JsonProperty("sageId")
        var sageId: String?,
        @JsonProperty("serialId")
        var serialId: String?,
        @JsonProperty("countryId")
        var countryId: String?,
        @JsonProperty("countryCode")
        var countryCode: String?,
        @JsonProperty("organizationSerialId")
        var organizationSerialId: String?,
        @JsonProperty("creditDays")
        var creditDays: String?,
        @JsonProperty("openInvoice")
        var openInvoice: List<DueAmount>?,
        @JsonProperty("onAccountPayment")
        var onAccountPayment: List<DueAmount>?,
        @JsonProperty("totalOutstanding")
        var totalOutstanding: List<DueAmount>?,
        @JsonProperty("openInvoiceCount")
        var openInvoiceCount: Int?,
        @JsonProperty("onAccountPaymentInvoiceCount")
        var onAccountPaymentInvoiceCount: Int?,
        @JsonProperty("totalOutstandingInvoiceCount")
        var totalOutstandingInvoiceCount: Int?,
        @JsonProperty("openInvoiceLedgerAmount")
        var openInvoiceLedgerAmount: BigDecimal?,
        @JsonProperty("onAccountPaymentInvoiceLedgerAmount")
        var onAccountPaymentInvoiceLedgerAmount: BigDecimal?,
        @JsonProperty("totalOutstandingInvoiceLedgerAmount")
        var totalOutstandingInvoiceLedgerAmount: BigDecimal?,
        @JsonProperty("notDueAmount")
        var notDueAmount: BigDecimal?,
        @JsonProperty("todayAmount")
        var todayAmount: BigDecimal?,
        @JsonProperty("thirtyAmount")
        var thirtyAmount: BigDecimal?,
        @JsonProperty("sixtyAmount")
        var sixtyAmount: BigDecimal?,
        @JsonProperty("ninetyAmount")
        var ninetyAmount: BigDecimal?,
        @JsonProperty("oneEightyAmount")
        var oneEightyAmount: BigDecimal?,
        @JsonProperty("threeSixtyFiveAmount")
        var threeSixtyFiveAmount: BigDecimal?,
        @JsonProperty("totalCreditNoteAmount")
        var totalCreditNoteAmount: BigDecimal?,
        @JsonProperty("totalDebitNoteAmount")
        var totalDebitNoteAmount: BigDecimal?,
        @JsonProperty("creditNoteCount")
        var creditNoteCount: Int?,
        @JsonProperty("debitNoteCount")
        var debitNoteCount: Int?,
        @JsonProperty("threeSixtyFivePlusAmount")
        var threeSixtyFivePlusAmount: BigDecimal?,
        @JsonProperty("notDueCount")
        var notDueCount: Int?,
        @JsonProperty("todayCount")
        var todayCount: Int?,
        @JsonProperty("thirtyCount")
        var thirtyCount: Int?,
        @JsonProperty("sixtyCount")
        var sixtyCount: Int?,
        @JsonProperty("ninetyCount")
        var ninetyCount: Int?,
        @JsonProperty("oneEightyCount")
        var oneEightyCount: Int?,
        @JsonProperty("threeSixtyFiveCount")
        var threeSixtyFiveCount: Int?,
        @JsonProperty("threeSixtyFivePlusCount")
        var threeSixtyFivePlusCount: Int?,
        @JsonProperty("updatedAt")
        var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now())
)
