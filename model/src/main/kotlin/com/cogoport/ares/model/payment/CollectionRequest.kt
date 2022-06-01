package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class CollectionRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String? = null,
    @QueryValue(AresModelConstants.ROLE) val role: String? = null,
    @QueryValue(AresModelConstants.QUARTER) val quarter: Int = AresModelConstants.CURR_QUARTER,
    @QueryValue(AresModelConstants.YEAR) val year: Int = AresModelConstants.CURR_YEAR
)
