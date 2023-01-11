package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class SupplierOutstandingReportResponse(
    @JsonProperty("organizationId")
    var organizationId: String?,
    @JsonProperty("selfOrganizationId")
    var selfOrganizationId: String?,
    @JsonProperty("businessName")
    var businessName: String?,
    @JsonProperty("registrationNumber")
    var registrationNumber: String?,
    @JsonProperty("collectionPartyType")
    var collectionPartyType: String?,
    @JsonProperty("companyType")
    var companyType: String?,
    @JsonProperty("supplyAgentId")
    var supplyAgentId: String?,
    @JsonProperty("supplyAgentName")
    var supplyAgentName: String?,
    @JsonProperty("supplyAgentEmail")
    var supplyAgentEmail: String?,
    @JsonProperty("supplyAgentMobileCountryCode")
    var supplyAgentMobileCountryCode: String?,
    @JsonProperty("supplyAgentMobileNumber")
    var supplyAgentMobileNumber: String?,
    @JsonProperty("sageId")
    var sageId: String?,
    @JsonProperty("countryId")
    var countryId: String?,
    @JsonProperty("countryCode")
    var countryCode: String?,
    @JsonProperty("category")
    var category: String?,
    @JsonProperty("serialId")
    var serialId: String?,
    @JsonProperty("organizationSerialId")
    var organizationSerialId: String?,
    @JsonProperty("creditDays")
    var creditDays: String?,
    @JsonProperty("openInvoice")
    var openInvoice: String?,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: String?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: String?,
    @JsonProperty("notDueAmount")
    var notDueAmount: String?,
    @JsonProperty("todayAmount")
    var todayAmount: String?,
    @JsonProperty("thirtyAmount")
    var thirtyAmount: String?,
    @JsonProperty("sixtyAmount")
    var sixtyAmount: String?,
    @JsonProperty("ninetyAmount")
    var ninetyAmount: String?,
    @JsonProperty("oneEightyAmount")
    var oneEightyAmount: String?,
    @JsonProperty("threeSixtyFiveAmount")
    var threeSixtyFiveAmount: String?,
    @JsonProperty("threeSixtyFivePlusAmount")
    var threeSixtyFivePlusAmount: String?
)
