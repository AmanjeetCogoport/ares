package com.cogoport.ares.api.settlement.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity
data class SettlementNumInfo(
    val settlementNum: String,
    val settlementDate: Timestamp
)
