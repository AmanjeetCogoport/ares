package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity
data class SettledInvoice(
    val id: Long?,
    val destinationId: Long,
    val documentValue: String,
    val destinationType: SettlementType,
    val accType: String?,
    val currency: String?,
    val currentBalance: BigDecimal?,
    val amount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val tds: BigDecimal? = BigDecimal.ZERO,
    val transactionDate: Date,
    var signFlag: Short,
    val settlementDate: Date,
    val exchangeRate: BigDecimal,
    var status: String?
)
