package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ReceivableAgeingResponse(
    @JsonProperty("zone")
    var zone: List<String>,
    @JsonProperty("receivableByAgeViaZone")
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>
)
