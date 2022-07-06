package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@MappedEntity(value = "settlements")
data class Settlement (
    val id: UUID?,
    val sourceId: BigInteger,
    val sourceType: SettlementType,
    val destinationId: BigInteger,
    val destinationType: SettlementType,
    val currency: String,
    val amount: BigDecimal,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    var signFlag: Short,
    val settlementDate: Date,
    val createdBy: String?,
    val createdAt: Timestamp?,
    val updatedBy: String?,
    val updatedAt: Timestamp?,
)