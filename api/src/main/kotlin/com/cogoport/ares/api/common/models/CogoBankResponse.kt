package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@JsonInclude(JsonInclude.Include.NON_NULL)
@Introspected
@ReflectiveAccess
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
data class CogoBankResponse(
    @JsonProperty("list")
    var bankList: List<CogoBanksDetails>
)
