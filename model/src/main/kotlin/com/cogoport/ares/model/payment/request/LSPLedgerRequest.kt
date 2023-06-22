package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class LSPLedgerRequest(
    val orgId: String,
    val year: String,
    val month: Int = 1,
    val entityCode: Int? = null
)
