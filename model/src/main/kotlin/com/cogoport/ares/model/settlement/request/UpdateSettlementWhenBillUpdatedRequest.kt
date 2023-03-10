package com.cogoport.ares.model.settlement.request

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateSettlementWhenBillUpdatedRequest(
        var sourceId: Long,
        var sourceType: String,
        var destinationId: Long,
        var destinationType: String,
        var currency: String,
        var amount: BigDecimal,
        var ledCurrency: String,
        var ledAmount: BigDecimal,
        var signFlag: Short?,
        var settlementDate: Timestamp,
        var updatedAt: Timestamp = Timestamp.from(Instant.now()),
        var updatedBy: UUID? = null,
)
