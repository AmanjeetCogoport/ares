package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerOutstandingAndOnAccountResponse(
    @JsonProperty("tradePartyDetailId")
    var tradePartyDetailId: UUID,
    @JsonProperty("entityCode")
    var entityCode: Int,
    @JsonProperty("currency")
    var currency: String,
    @JsonProperty("tradePartyDetailName")
    var tradePartyDetailName: String,
    @JsonProperty("tradePartyMappingId")
    var tradePartyMappingId: UUID,
    @JsonProperty("taggedOrganizationId")
    var taggedOrganizationId: UUID,
    @JsonProperty("outstandingAmount")
    var outstandingAmount: BigDecimal,
    @JsonProperty("onAccountAmount")
    var onAccountAmount: BigDecimal
)
