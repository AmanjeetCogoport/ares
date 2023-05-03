package com.cogoport.ares.api.migration.model
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class SerialIdDetailsRequest(
    @JsonProperty("organization_trade_party_mappings")
    var organizationTradePartyMappings: List<SerialIdsInput>,
    @JsonProperty("cogo_entity_id")
    var cogoEntityId: String?
)
