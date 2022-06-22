package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformOrganization(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("serial_id")
    val serialId: Long,

    @JsonProperty("registration_number")
    val registrationNumber: String,

    @JsonProperty("business_name")
    val businessName: String?,

    @JsonProperty("short_name")
    val shortName: String?,

    @JsonProperty("trade_name")
    val tradeName: String?,

    @JsonProperty("account_type")
    val accountType: String?,

    @JsonProperty("status")
    val status: String?,

    @JsonProperty("kyc_status")
    val kycStatus: String?,

    @JsonProperty("partnerId")
    val partner_id: String?,

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
