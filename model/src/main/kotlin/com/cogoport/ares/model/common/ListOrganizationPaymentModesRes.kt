package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected

@Introspected
data class ListOrganizationPaymentModesRes(
    var list: List<OrganizationPaymentMode>? = listOf(),
    val page: Int? = 1,
    val total: Int? = 1,
    val totalCount: Int? = 1,
    val pageLimit: Int? = 10
)
