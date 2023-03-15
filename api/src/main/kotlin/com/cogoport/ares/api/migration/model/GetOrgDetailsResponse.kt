package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class GetOrgDetailsResponse(
    @JsonProperty("organization_id")
    val organizationId: String? = null,
    @JsonProperty("zone")
    val zone: String? = null,
    @JsonProperty("trade_party_detail_serial_id")
    val tradePartySerialId: String? = null,
    @JsonProperty("organization_trade_party_detail_id")
    val organizationTradePartyDetailId: UUID?= null
)
