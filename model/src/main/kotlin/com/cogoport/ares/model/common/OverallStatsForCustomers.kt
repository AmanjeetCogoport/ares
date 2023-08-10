package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotEmpty

@Introspected
data class OverallStatsForCustomers(
    @field: NotEmpty
    val bookingPartyIds: List<String>?
)
