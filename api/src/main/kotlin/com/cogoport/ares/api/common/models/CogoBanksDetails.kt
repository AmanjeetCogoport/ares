package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.sql.Timestamp
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@JsonInclude
data class CogoBanksDetails(
    @JsonProperty("id")
    var id: String? = null,
    @JsonProperty("serial_id")
    var serialId: Long? = 0L,
    @JsonProperty("business_name")
    var businessName: String? = null,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("logo")
    var logo: String? = null,
    @JsonProperty("registration_number")
    var registrationNumber: String? = null,
    @JsonProperty("country_id")
    var countryId: UUID? = null,
    @JsonProperty("is_incentivised")
    var incentivised: Boolean? = null,
    @JsonProperty("serviceable_location_ids")
    var serviceableLocationIds: Any? = null,
    @JsonProperty("roles")
    var roles: Any? = null,
    @JsonProperty("agreement")
    var agreement: Any? = null,
    @JsonProperty("commercials")
    var commercials: Any? = null,
    @JsonProperty("first_loss_default_guarantee")
    var firstLossDefaultGuarantee: Any? = null,
    @JsonProperty("first_loss_default_guarantee_currency")
    var firstLossDefaultGuaranteeCurrency: Any? = null,
    @JsonProperty("shipping_line_ids")
    var shippingLineIds: Any? = null,
    @JsonProperty("created_at")
    var createdAt: Timestamp? = null,
    @JsonProperty("updated_at")
    var updatedAt: Timestamp? = null,
    @JsonProperty("entity_types")
    var entityTypes: Any? = null,
    @JsonProperty("parent_entity_id")
    var parentEntityId: Any? = null,
    @JsonProperty("entity_manager_id")
    var entityManagerId: Any? = null,
    @JsonProperty("business_address_proof")
    var businessAddressProof: Any? = null,
    @JsonProperty("remarks")
    var remarks: Any? = null,
    @JsonProperty("role_ids")
    var roleIds: Any? = null,
    @JsonProperty("entity_code")
    var entityCode: String? = null,
    @JsonProperty("allowed_country_ids")
    var allowedCountryIds: String? = null,
    @JsonProperty("cin")
    var cin: String? = null,
    @JsonProperty("tan_no")
    var tanNo: String? = null,
    @JsonProperty("addresses")
    var addresses: List<Addresses>? = null,
    @JsonProperty("bank_details")
    var bankDetails: List<BankDetails>? = null,
    @JsonProperty("country")
    var country: List<Country>? = null,

)
