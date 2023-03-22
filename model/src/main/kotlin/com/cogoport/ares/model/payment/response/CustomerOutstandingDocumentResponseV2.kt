package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AgeingBucketOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.sql.Timestamp
import java.time.LocalDateTime

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@ReflectiveAccess
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
    @JsonProperty("creditNote")
    var creditNote: List<DueAmount>?,
    @JsonProperty("debitNote")
    var debitNote: List<DueAmount>?,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: List<DueAmount>?,
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @JsonProperty("totalOutstanding")
    var totalOutstanding: List<DueAmount>?,
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int?,
    @JsonProperty("entityCode")
    var entityCode: Int?,
)
