package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class TicketGenerationResp(
    @JsonProperty("TicketToken")
    var ticketToken: String
)
