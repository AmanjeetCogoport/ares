package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class TradePartyResponse(
    @JsonProperty("mapping_id")
    var mappingId: UUID?,

    @JsonProperty("trade_party_detail_id")
    var tradePartyDetailId: UUID?,

    @JsonProperty("trade_party_detail_serial_id")
    var tradePartyDetailSerialId: Long?,

    @JsonProperty("legal_business_name")
    var legalBusinessName: String?
)
