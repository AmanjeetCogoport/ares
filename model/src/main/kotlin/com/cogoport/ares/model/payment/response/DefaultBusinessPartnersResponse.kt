package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class DefaultBusinessPartnersResponse(
    @JsonProperty("id")
    var id: String,
    @JsonProperty("businessName")
    var businessName: String,
    @JsonProperty("tradePartyDetailSerialId")
    var tradePartyDetailSerialId: Long,
    @JsonProperty("sageOrgId")
    var sageOrgId: String
)
