package com.cogoport.ares.api.settlement.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@Introspected
@MappedEntity
data class PaymentInfo(
    val entityCode: Long? = null,
    val bankId: UUID? = null,
    val bankName: String? = null,
    val transRefNumber: String? = null,
    val payMode: String? = null,
    val settlementDate: Timestamp,
    val settlementNum: String
)
