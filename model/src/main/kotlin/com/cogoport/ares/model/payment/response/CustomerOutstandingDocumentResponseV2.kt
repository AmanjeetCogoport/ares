package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class CustomerOutstandingDocumentResponseV2(
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
    @JsonProperty("openInvoiceAgeingBucket")
    var openInvoiceAgeingBucket: List<AgeingBucketOutstandingV2>?,
    @JsonProperty("onAccountAgeingBucket")
    var onAccountAgeingBucket: List<AgeingBucketOutstandingV2>?,
    @JsonProperty("creditNoteAgeingBucket")
    var creditNoteAgeingBucket: List<AgeingBucketOutstandingV2>?,
    @JsonProperty("entityCode")
    var entityCode: Int?,
    @JsonProperty("lastUpdatedAt")
    var lastUpdatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @JsonProperty("totalCallPriorityScore")
    var totalCallPriorityScore: Int? = 0,
    @JsonProperty("creditController")
    var creditController: List<SupplyAgentV3>?,
    @JsonProperty("kam")
    var kam: List<SupplyAgentV3>?,
    @JsonProperty("salesAgent")
    var salesAgent: List<SupplyAgentV3>?,
    @JsonProperty("cogoEntityId")
    var cogoEntityId: UUID?,
    @JsonProperty("portfolioManager")
    var portfolioManager: List<SupplyAgentV3>?,
    @JsonProperty("taggedState")
    var taggedState: String?,
    @JsonProperty("openInvoiceAmount")
    var openInvoiceAmount: BigDecimal?,
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int?,
    @JsonProperty("onAccount")
    var onAccountAmount: BigDecimal?,
    @JsonProperty("onAccountCount")
    var onAccountCount: Int?,
    @JsonProperty("creditNoteAmount")
    var creditNoteAmount: BigDecimal?,
    @JsonProperty("creditNoteCount")
    var creditNoteCount: Int?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: BigDecimal?,
)

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SupplyAgentV3(
    @JsonProperty("id")
    var id: UUID?,
    @JsonProperty("name")
    var name: String?,
    @JsonProperty("email")
    var email: String?,
    @JsonProperty("mobileCountryCode")
    var mobileCountryCode: String?,
    @JsonProperty("mobileNumber")
    var mobileNumber: String?,
    @JsonProperty("rmDetails")
    var rmDetails: SupplyAgent
)

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AgeingBucketOutstandingV2(
    @JsonProperty("ledgerAmount")
    var ledgerAmount: BigDecimal,
    @JsonProperty("ledgerCount")
    var ledgerCount: Long?,
    @JsonProperty("ledgerCurrency")
    var ledgerCurrency: String,
    @JsonProperty("key")
    var key: String,
)
