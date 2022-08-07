package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class CogoBankInfo(
    val bankCode: String,
    val cogoAccountNo: String,
    val bankName: String,
    val bankId: UUID
)
