package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AgeingBucketOutstanding
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.time.LocalDateTime

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class CustomerOutstandingDocumentResponse(
    @JsonProperty("organizationId")
    var organizationId: String?,
    @JsonProperty("selfOrganizationId")
    var tradePartyId: String?,
    @JsonProperty("businessName")
    var businessName: String?,
    @JsonProperty("selfOrganizationName")
    var tradePartyName: String?,
    @JsonProperty("collectionPartyType")
    var tradePartyType: List<String?>?,
    @JsonProperty("registrationNumber")
    var registrationNumber: String?,
    @JsonProperty("companyType")
    var companyType: String?,
    @JsonProperty("creditController")
    var creditController: SupplyAgent?,
    @JsonProperty("kam")
    var kam: SupplyAgent?,
    @JsonProperty("salesAgent")
    var salesAgent: SupplyAgent?,
    @JsonProperty("sageId")
    var sageId: String?,
    @JsonProperty("serialId")
    var tradePartySerialId: String?,
    @JsonProperty("countryId")
    var countryId: String?,
    @JsonProperty("countryCode")
    var countryCode: String?,
    @JsonProperty("organizationSerialId")
    var organizationSerialId: String?,
    @JsonProperty("creditDays")
    var creditDays: String?,
    @JsonProperty("ageingBucket")
    var ageingBucket: HashMap<String, AgeingBucketOutstanding>?,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: AgeingBucketOutstanding?,
    @JsonProperty("lastUpdatedAt")
    var lastUpdatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @JsonProperty("totalOutstanding")
    var totalOutstanding: AgeingBucketOutstanding?,
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int?,
    @JsonProperty("entityCode")
    var entityCode: Int?,
)
