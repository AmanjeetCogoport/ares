package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class PaidUnpaidStatus(
    val documentNumber: Long,
    val documentValue: String,
    val status: String,
)
