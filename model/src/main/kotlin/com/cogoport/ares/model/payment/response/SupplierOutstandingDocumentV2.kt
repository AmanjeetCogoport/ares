package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AccMode
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
data class SupplierOutstandingDocumentV2(
    @JsonProperty("organizationId")
    val organizationId: UUID?,

    @JsonProperty("entityCode")
    val entityCode: Int?,

    @JsonProperty("ledCurrency")
    val ledCurrency: String?,

    @JsonProperty("organizationName")
    val organizationName: String?,

    @JsonProperty("accMode")
    val accMode: AccMode?,

    @JsonProperty("invoiceNotDueAmount")
    val invoiceNotDueAmount: BigDecimal?,

    @JsonProperty("invoiceTodayAmount")
    val invoiceTodayAmount: BigDecimal?,

    @JsonProperty("invoiceThirtyAmount")
    val invoiceThirtyAmount: BigDecimal?,

    @JsonProperty("invoiceSixtyAmount")
    val invoiceSixtyAmount: BigDecimal?,

    @JsonProperty("invoiceNinetyAmount")
    val invoiceNinetyAmount: BigDecimal?,

    @JsonProperty("invoiceOneEightyAmount")
    val invoiceOneEightyAmount: BigDecimal?,

    @JsonProperty("invoiceThreeSixtyFiveAmount")
    val invoiceThreeSixtyFiveAmount: BigDecimal?,

    @JsonProperty("invoiceThreeSixtyFivePlusAmount")
    val invoiceThreeSixtyFivePlusAmount: BigDecimal?,

    @JsonProperty("invoiceNotDueCount")
    val invoiceNotDueCount: Long?,

    @JsonProperty("invoiceTodayCount")
    val invoiceTodayCount: Long?,

    @JsonProperty("invoiceThirtyCount")
    val invoiceThirtyCount: Long?,

    @JsonProperty("invoiceSixtyCount")
    val invoiceSixtyCount: Long?,

    @JsonProperty("invoiceNinetyCount")
    val invoiceNinetyCount: Long?,

    @JsonProperty("invoiceOneEightyCount")
    val invoiceOneEightyCount: Long?,

    @JsonProperty("invoiceThreeSixtyFiveCount")
    val invoiceThreeSixtyFiveCount: Long?,

    @JsonProperty("invoiceThreeSixtyFivePlusCount")
    val invoiceThreeSixtyFivePlusCount: Long?,

    @JsonProperty("creditNoteCount")
    val creditNoteCount: Long?,

    @JsonProperty("onAccountNotDueAmount")
    val onAccountNotDueAmount: BigDecimal?,

    @JsonProperty("onAccountTodayAmount")
    val onAccountTodayAmount: BigDecimal?,

    @JsonProperty("onAccountThirtyAmount")
    val onAccountThirtyAmount: BigDecimal?,

    @JsonProperty("onAccountSixtyAmount")
    val onAccountSixtyAmount: BigDecimal?,

    @JsonProperty("onAccountNinetyAmount")
    val onAccountNinetyAmount: BigDecimal?,

    @JsonProperty("onAccountOneEightyAmount")
    val onAccountOneEightyAmount: BigDecimal?,

    @JsonProperty("onAccountThreeSixtyFiveAmount")
    val onAccountThreeSixtyFiveAmount: BigDecimal?,

    @JsonProperty("onAccountThreeSixtyFivePlusAmount")
    val onAccountThreeSixtyFivePlusAmount: BigDecimal?,

    @JsonProperty("onAccountNotDueCount")
    val onAccountNotDueCount: Long?,

    @JsonProperty("onAccountTodayCount")
    val onAccountTodayCount: Long?,

    @JsonProperty("onAccountThirtyCount")
    val onAccountThirtyCount: Long?,

    @JsonProperty("onAccountSixtyCount")
    val onAccountSixtyCount: Long?,

    @JsonProperty("onAccountNinetyCount")
    val onAccountNinetyCount: Long?,

    @JsonProperty("onAccountOneEightyCount")
    val onAccountOneEightyCount: Long?,

    @JsonProperty("onAccountThreeSixtyFiveCount")
    val onAccountThreeSixtyFiveCount: Long?,

    @JsonProperty("onAccountThreeSixtyFivePlusCount")
    val onAccountThreeSixtyFivePlusCount: Long?,

    @JsonProperty("notDueOutstanding")
    val notDueOutstanding: BigDecimal?,

    @JsonProperty("todayOutstanding")
    val todayOutstanding: BigDecimal?,

    @JsonProperty("thirtyOutstanding")
    val thirtyOutstanding: BigDecimal?,

    @JsonProperty("sixtyOutstanding")
    val sixtyOutstanding: BigDecimal?,

    @JsonProperty("ninetyOutstanding")
    val ninetyOutstanding: BigDecimal?,

    @JsonProperty("oneEightyOutstanding")
    val oneEightyOutstanding: BigDecimal?,

    @JsonProperty("threeSixtyFiveOutstanding")
    val threeSixtyFiveOutstanding: BigDecimal?,

    @JsonProperty("threeSixtyFivePlusOutstanding")
    val threeSixtyFivePlusOutstanding: BigDecimal?,

    @JsonProperty("totalOpenInvoiceAmount")
    val totalOpenInvoiceAmount: BigDecimal?,

    @JsonProperty("totalCreditNoteAmount")
    val totalCreditNoteAmount: BigDecimal?,

    @JsonProperty("totalOpenOnAccountAmount")
    val totalOpenOnAccountAmount: BigDecimal?,

    @JsonProperty("totalOutstanding")
    val totalOutstanding: BigDecimal?,

    @JsonProperty("totalInvoiceAmount")
    val totalInvoiceAmount: BigDecimal?,

    @JsonProperty("totalOnAccountAmount")
    val totalOnAccountAmount: BigDecimal?,

    @JsonProperty("totalOnAccountCount")
    val totalOnAccountCount: Long?,

    @JsonProperty("totalInvoicesCount")
    val totalInvoicesCount: Long?,

    @JsonProperty("createdAt")
    val createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),

    @JsonProperty("registrationNumber")
    val registrationNumber: String?,

    @JsonProperty("bpr")
    val bpr: String?,

    @JsonProperty("tradeType")
    var tradeType: List<String?>? = null,

    @JsonProperty("organizationSerialId")
    val organizationSerialId: String?,

    @JsonProperty("agent")
    var agent: List<SupplyAgentV2?>? = null,

    @JsonProperty("companyType")
    val companyType: String?,

    @JsonProperty("tradePartySerialId")
    val tradePartySerialId: Long?,

    @JsonProperty("closingOutstandingAmountAtFirstApril")
    val closingOutstandingAmountAtFirstApril: BigDecimal?,

    @JsonProperty("closingInvoiceAmountAtFirstApril")
    val closingInvoiceAmountAtFirstApril: BigDecimal?,

    @JsonProperty("closingCreditNoteAmountAtFirstApril")
    val closingCreditNoteAmountAtFirstApril: BigDecimal?,

    @JsonProperty("closingOnAccountAmountAtFirstApril")
    val closingOnAccountAmountAtFirstApril: BigDecimal?,

    @JsonProperty("creditDays")
    val creditDays: Long?,

    @JsonProperty("countryId")
    val countryId: UUID?,

    @JsonProperty("totalOpenInvoiceCount")
    var totalOpenInvoiceCount: Long?,

    @JsonProperty("totalOpenOnAccountCount")
    var totalOpenOnAccountCount: Long?,

    @JsonProperty("countryCode")
    var countryCode: String?
)
