package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
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
    val sourceType: AccountType,
    val destinationId: BigInteger,
    val destinationType: AccountType,
    val currency: String,
    val amount: BigDecimal,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    val settlementDate: Date,
    val createdBy: String?,
    val createdAt: Timestamp?,
    val updatedBy: String?,
    val updatedAt: Timestamp?,
)