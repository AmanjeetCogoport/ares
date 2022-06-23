package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlatformOrganizationDetails(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("serial_id")
    val serialId: Int,
    @JsonProperty("registration_number")
    val registrationNumber: String,
    @JsonProperty("query_name")
    val queryName: String?,
    @JsonProperty("business_name")
    val businessName: String?,
    @JsonProperty("short_name")
    val shortName: String?,
    @JsonProperty("trade_name")
    val tradeName: String?,
    @JsonProperty("account_type")
    val accountType: String?,
    @JsonProperty("logo")
    val logo: String?,
    @JsonProperty("status")
    val status: String?,
    @JsonProperty("kyc_status")
    val kycStatus: String?,
    @JsonProperty("kyc_rejection_reason")
    val kycRejectionReason: String?,
    @JsonProperty("companySize")
    val company_size: String?,
    @JsonProperty("import_export_types")
    val importExportTypes: List<String>?,
    @JsonProperty("logistics_modes")
    val logisticsModes: List<String>?,
    @JsonProperty("list")
    val industries: List<String>?,
    @JsonProperty("partnerId")
    val partner_id: String?,
    @JsonProperty("createdAt")
    val created_at: String?,
    @JsonProperty("updatedAt")
    val updated_at: String?,
    @JsonProperty("tds_deduction_style")
    val tdsDeductionStyle: String?,
    @JsonProperty("tds_deduction_type")
    val tdsDeductionType: String?,
    @JsonProperty("tds_deduction_rate")
    val tdsDeductionRate: String?,
    @JsonProperty("kyc_verified_at")
    val kycVerifiedAt: String?,
    @JsonProperty("commodities")
    val commodities: List<String>?,
    @JsonProperty("company_type")
    val companyType: String?,
    @JsonProperty("sage_company_id")
    val sageCompanyId: String?,
    @JsonProperty("cogo_entity_id")
    val cogoEntityId: String?,
    @JsonProperty("category_types")
    val categoryTypes: String?,
)
