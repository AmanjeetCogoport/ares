package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.DueAmount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
data class SupplierOutstandingResponse(
    @JsonProperty("organizationId")
    var organizationId: String?,
    @JsonProperty("legalName")
    var legalName: String?,
    @JsonProperty("businessName")
    var businessName: String?,
    @JsonProperty("taxNumber")
    var taxNumber: String?,
    @JsonProperty("collectionPartyType")
    var collectionPartyType: String?,
    @JsonProperty("supplyAgent")
    var supplyAgent: String?,
    @JsonProperty("sageId")
    var sageId: String?,
    @JsonProperty("cogoEntityId")
    var cogoEntityId: String?,
    @JsonProperty("countryId")
    var countryId: String?,
    @JsonProperty("countryCode")
    var countryCode: String?,
    @JsonProperty("category")
    var category: String?,
    @JsonProperty("serialId")
    var serialId: String?,
    @JsonProperty("creditDays")
    var creditDays: String?,
    @JsonProperty("openInvoiceAmountDue")
    var openInvoiceAmountDue: List<DueAmount>?,
    @JsonProperty("onAccountPaymentAmountDue")
    var onAccountPaymentAmountDue: List<DueAmount>?,
    @JsonProperty("totalOutstandingAmountDue")
    var totalOutstandingAmountDue: List<DueAmount>?,
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int?,
    @JsonProperty("onAccountPaymentInvoiceCount")
    var onAccountPaymentInvoiceCount: Int?,
    @JsonProperty("totalOutstandingInvoiceCount")
    var totalOutstandingInvoiceCount: Int?,
    @JsonProperty("openInvoiceLedAmount")
    var openInvoiceLedAmount: BigDecimal?,
    @JsonProperty("onAccountPaymentInvoiceLedAmount")
    var onAccountPaymentInvoiceLedAmount: BigDecimal?,
    @JsonProperty("totalOutstandingInvoiceLedAmount")
    var totalOutstandingInvoiceLedAmount: BigDecimal?,
    @JsonProperty("notDueAmount")
    var notDueAmount: BigDecimal?,
    @JsonProperty("todayAmount")
    var todayAmount: BigDecimal?,
    @JsonProperty("thirtyAmount")
    var thirtyAmount: BigDecimal?,
    @JsonProperty("sixtyAmount")
    var sixtyAmount: BigDecimal?,
    @JsonProperty("nintyAmount")
    var nintyAmount: BigDecimal?,
    @JsonProperty("oneEightyAmount")
    var oneEightyAmount: BigDecimal?,
    @JsonProperty("threeSixtyFiveAmount")
    var threeSixtyFiveAmount: BigDecimal?,
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
    @JsonProperty("nintyCount")
    var nintyCount: Int?,
    @JsonProperty("oneEightyCount")
    var oneEightyCount: Int?,
    @JsonProperty("threeSixtyFiveCount")
    var threeSixtyFiveCount: Int?,
    @JsonProperty("threeSixtyFivePlusCount")
    var threeSixtyFivePlusCount: Int?,
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp?,
)
