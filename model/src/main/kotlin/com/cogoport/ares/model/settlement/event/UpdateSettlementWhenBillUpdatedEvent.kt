package com.cogoport.ares.model.settlement.event

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateSettlementWhenBillUpdatedEvent(
        var billId: Long,
        var billNumber: String,
        var accUtilId: Long,
        var updatedBy: UUID?
)
