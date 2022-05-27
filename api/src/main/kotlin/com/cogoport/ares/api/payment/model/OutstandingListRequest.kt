package com.cogoport.ares.api.payment.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class OutstandingListRequest(
    @QueryValue("zone") val zone: String?,
    @QueryValue("role") val role: String?,
    @QueryValue("page") val page: Int = 1,
    @QueryValue("page_limit") val page_limit: Int = 10,
)
