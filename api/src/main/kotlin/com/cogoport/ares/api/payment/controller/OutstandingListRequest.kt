package com.cogoport.ares.api.payment.controller

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.QueryValue

@Introspected
data class OutstandingListRequest(
    @QueryValue("zone") val zone: String?,
    @QueryValue("role") val role: String?,
    @QueryValue("page") val page: Int = 1,
    @Nullable @QueryValue("page_limit") val page_limit: Int = 10,
    @QueryValue("quarter") val quarter: Int
)
