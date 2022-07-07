package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@MappedEntity(value = "settlements")
data class Settlement(
    @field:Id @GeneratedValue
    val id: UUID?,
    val sourceId: Long,
    val sourceType: SettlementType,
    val destinationId: Long,
    val destinationType: SettlementType,
    val currency: String?,
    val amount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    var signFlag: Short,
    val settlementDate: Date,
    val createdBy: UUID?,
    val createdAt: Timestamp?,
    val updatedBy: UUID?,
    val updatedAt: Timestamp?,
)
