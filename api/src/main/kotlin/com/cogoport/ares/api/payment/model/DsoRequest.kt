package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
@Introspected
data class DsoRequest(
    @QueryValue(AresConstants.ZONE) val zone: String? = null,
    @QueryValue(AresConstants.ROLE) val role: String? = null,
    @QueryValue(AresConstants.QUARTER) val quarter: List<Int> = listOf(AresConstants.CURR_QUARTER),
    @QueryValue(AresConstants.YEAR) val year: Int = AresConstants.CURR_YEAR
)
