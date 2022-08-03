package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class TradePartyOrganizationResponse(

    @JsonProperty("organization_trade_party_detail_id")
    var organizationTradePartyDetailId: UUID?,

    @JsonProperty("organization_trade_party_name")
    var organizationTradePartyName: String?,

    @JsonProperty("organization_trade_party_serial_id")
    var organizationTradePartySerialId: Long?,

    @JsonProperty("organization_trade_party_zone")
    var organizationTradePartyZone: String?
)
