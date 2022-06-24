package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp
import java.util.UUID

@JsonInclude
data class Addresses(
    @JsonProperty("id")
    var id: String? = null,
    @JsonProperty("partner_id")
    var partnerId: UUID? = null,
    @JsonProperty("gst_number")
    var gstNumber: String? = null,
    @JsonProperty("address")
    var address: String? = null,
    @JsonProperty("pin_code")
    var pinCode: Int? = null,
    @JsonProperty("city_id")
    var cityId: String? = null,
    @JsonProperty("country_id")
    var countryId: String? = null,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("created_at")
    var createdAt: Timestamp? = null,
    @JsonProperty("updated_at")
    var updatedAt: Timestamp? = null,
    @JsonProperty("region_id")
    var regionId: String? = null,
    @JsonProperty("country")
    var country: Country? = null,
    @JsonProperty("city")
    var city: City? = null,
)
