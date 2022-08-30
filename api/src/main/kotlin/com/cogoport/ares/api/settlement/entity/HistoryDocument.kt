package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity
data class HistoryDocument(
    val id: Long?,
    val documentNo: Long,
    val documentValue: String,
    val accType: String?,
    val currency: String?,
    val currentBalance: BigDecimal?,
    val amount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val transactionDate: Date,
    var signFlag: Short,
    val exchangeRate: BigDecimal,
    val settledAmount: BigDecimal,
    val lastEditedDate: Date,
    var status: String?,
    val settledTds: BigDecimal,
    val accMode: AccMode
)
