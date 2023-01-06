package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.kuber.model.GetUserResponse
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
    @JsonProperty("businessName")
    var businessName: String?,
    @JsonProperty("taxNumber")
    var taxNumber: String?,
    @JsonProperty("collectionPartyType")
    var collectionPartyType: List<String?>,
    @JsonProperty("companyType")
    var companyType: String?,
    @JsonProperty("supplyAgent")
    var supplyAgent: GetUserResponse?,
    @JsonProperty("sageId")
    var sageId: String?,
    @JsonProperty("countryId")
    var countryId: String?,
    @JsonProperty("countryCode")
    var countryCode: String?,
    @JsonProperty("category")
    var category: List<String?>,
    @JsonProperty("serialId")
    var serialId: String?,
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
    @JsonProperty("ninetyAmount")
    var ninetyAmount: BigDecimal?,
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
    @JsonProperty("ninetyCount")
    var ninetyCount: Int?,
    @JsonProperty("oneEightyCount")
    var oneEightyCount: Int?,
    @JsonProperty("threeSixtyFiveCount")
    var threeSixtyFiveCount: Int?,
    @JsonProperty("threeSixtyFivePlusCount")
    var threeSixtyFivePlusCount: Int?,
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp?,
)
