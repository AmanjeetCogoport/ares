package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.InvoiceStats
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
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
    @JsonProperty("openInvoices")
    var openInvoices: InvoiceStats?,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: InvoiceStats?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: InvoiceStats?,
    @JsonProperty("ageingBucket")
    var ageingBucket: List<AgeingBucket>?,
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp?,
)
