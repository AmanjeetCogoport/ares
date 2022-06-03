package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
@Introspected
data class AccountCollectionRequest(
    @QueryValue("startDate") val startDate: String? = null,
    @QueryValue("endDate") val endDate: String? = null,
    @QueryValue("entityType") val entityType: Int? = null,
    @QueryValue("currencyType") val currencyType: String? = null,
    @QueryValue("page") val page: Int = 1,
    @QueryValue("pageLimit") val pageLimit: Int  = 10
)