package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
data class OrganizationTradePartyDetails(
    @JsonProperty("organization_trade_party_detail_id")
    val organizationTradePartDetailId: UUID?,
    @JsonProperty("registration_number")
    val registrationNumber: String?,
    @JsonProperty("partner_id")
    val partnerId: String?,
    @JsonProperty("cogo_entity_id")
    val cogoEntityId: String?,
    @JsonProperty("organization_id")
    val organizationId: String?,
)
