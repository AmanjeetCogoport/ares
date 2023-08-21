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
    @JsonProperty("openInvoice")
    var openInvoice: AgeingBucketOutstanding?,
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int?,
    @JsonProperty("openInvoiceAgeingBucket")
    var openInvoiceAgeingBucket: HashMap<String, AgeingBucketOutstanding>?,
    @JsonProperty("onAccount")
    var onAccount: AgeingBucketOutstanding?,
    @JsonProperty("onAccountCount")
    var onAccountCount: Int?,
    @JsonProperty("onAccountAgeingBucket")
    var onAccountAgeingBucket: HashMap<String, AgeingBucketOutstanding>?,
    @JsonProperty("creditNote")
    var creditNote: AgeingBucketOutstanding?,
    @JsonProperty("creditNoteAgeingBucket")
    var creditNoteAgeingBucket: HashMap<String, AgeingBucketOutstanding>?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: AgeingBucketOutstanding?,
    @JsonProperty("entityCode")
    var entityCode: Int?,
    @JsonProperty("lastUpdatedAt")
    var lastUpdatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @JsonProperty("totalCallPriorityScore")
    var totalCallPriorityScore: Int? = 0
)
