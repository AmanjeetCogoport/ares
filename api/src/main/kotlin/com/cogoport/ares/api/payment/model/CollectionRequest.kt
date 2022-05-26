package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.QueryValue

@Introspected
data class CollectionRequest(
    @QueryValue("zone")
    val zone: String?,
    @QueryValue(AresConstants.ROLE)
    val role: String?,
    @QueryValue(AresConstants.QUARTER)
    val quarter: Int,
    @QueryValue(AresConstants.YEAR)
    val year: Int
)
