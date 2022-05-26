package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import javax.annotation.Nullable

@Introspected
data class MonthlyOutstandingRequest(
    @QueryValue(AresConstants.ZONE) var zone: String?,
    @QueryValue(AresConstants.ROLE) val role: String?
)