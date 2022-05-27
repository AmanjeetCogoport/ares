package com.cogoport.ares.api.payment.model

import io.micronaut.http.annotation.QueryValue

data class PushToDashboardRequest (
    @QueryValue("zone") val zone: String?,
    @QueryValue("date") val date: String,
    @QueryValue("quarter") val quarter: Int?,
    @QueryValue("year") val year: Int
)