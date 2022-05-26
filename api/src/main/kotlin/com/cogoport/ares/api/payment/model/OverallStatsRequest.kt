package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.bind.annotation.Bindable
import io.micronaut.http.annotation.QueryValue
@Introspected

data class OverallStatsRequest(
    @QueryValue(AresConstants.ZONE) val zone: String?,
    @QueryValue(AresConstants.ROLE) val role: String?
)
