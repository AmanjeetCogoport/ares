package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class CommunicationResp(
    @JsonProperty("id")
    var id: String?
)
