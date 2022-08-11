package com.cogoport.ares.api.migration.model
import com.fasterxml.jackson.annotation.JsonProperty

data class SerialIdsInput(
    @JsonProperty("organization_serial_id")
    val organizationSerialId: Long,
    @JsonProperty("trade_party_detail_serial_id")
    val tradePartyDetailSerialId: Long
)
