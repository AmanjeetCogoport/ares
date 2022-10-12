package com.cogoport.ares.api.migration.model

import com.cogoport.ares.model.payment.OrganizationDetails
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class SerialIdDetailsResponse(
    @JsonProperty("id")
    val mappingId: UUID,
    @JsonProperty("organization_id")
    val organizationId: UUID,
    @JsonProperty("organization_trade_party_detail_id")
    val organizationTradePartyDetailId: UUID,
    @JsonProperty("business_name")
    val tradePartyBusinessName: String,
    @JsonProperty("serial_id")
    val tradePartySerial: Long,
    @JsonProperty("organization")
    val organization: OrganizationDetails?
)
