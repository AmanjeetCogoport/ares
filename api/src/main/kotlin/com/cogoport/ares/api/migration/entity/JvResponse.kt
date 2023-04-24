package com.cogoport.ares.api.migration.entity

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class JvResponse(
    val jvId: Long,
    val accountUtilizationId: Long,
    val amountLedger: BigDecimal,
    val payLedger: BigDecimal,
    val ledgerCurrency: String,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp
)
