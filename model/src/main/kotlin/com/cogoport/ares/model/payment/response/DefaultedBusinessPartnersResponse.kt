package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class DefaultedBusinessPartnersResponse(
    @JsonProperty("id")
    var id: String,
    @JsonProperty("businessName")
    var businessName: String,
    @JsonProperty("tradePartyDetailSerialId")
    var tradePartyDetailSerialId: String,
    @JsonProperty("sageOrgId")
    var sageOrgId: String
)
