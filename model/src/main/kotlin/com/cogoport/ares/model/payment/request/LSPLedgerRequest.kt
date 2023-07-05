package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class LSPLedgerRequest(
    val orgId: String,
    val year: Int,
    val month: String,
    var entityCode: Int? = null,
    var pageLimit: Int?,
    var page: Int?
)
