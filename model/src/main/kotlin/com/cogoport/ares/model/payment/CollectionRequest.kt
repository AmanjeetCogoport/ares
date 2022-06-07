package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class CollectionRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String? = null,
    @QueryValue(AresModelConstants.ROLE) val role: String? = null,
    @QueryValue(AresModelConstants.QUARTER_YEAR) val quarterYear: String = ("Q" + AresModelConstants.CURR_QUARTER + "_" + AresModelConstants.CURR_YEAR)
)
