package com.cogoport.ares.api.exception

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ErrorResponse(
    @JsonProperty("status")
    var status: Int = 100,

    @JsonProperty("message")
    var errorMessage: String? = "",
)
