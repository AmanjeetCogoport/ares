package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class MonthlyOutstandingRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String?,
    @QueryValue(AresModelConstants.ROLE) val role: String?
)
