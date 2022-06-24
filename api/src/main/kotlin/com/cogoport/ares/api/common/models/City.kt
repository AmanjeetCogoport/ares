package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude
data class City(
    @JsonProperty("id")
    var id: String? = null,
    @JsonProperty("name")
    var name: String? = null,
    @JsonProperty("country_code")
    var countryCode: String? = null,
    @JsonProperty("type")
    var type: String? = null,
)
