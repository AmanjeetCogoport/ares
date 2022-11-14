package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDefaultedBusinessPartnersRequest(
    var q: String? = null,
    var page: Int? = 1,
    var pageLimit: Int? = 10
)
