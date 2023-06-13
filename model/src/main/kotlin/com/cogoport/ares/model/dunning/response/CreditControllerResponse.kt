package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreditControllerResponse(
    @JsonProperty("credit_controller_id")
    var creditControllerId: UUID,
    @JsonProperty("credit_controller_name")
    var creditControllerName: String
)
