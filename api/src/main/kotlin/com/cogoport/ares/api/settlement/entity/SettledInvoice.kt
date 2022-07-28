package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@MappedEntity
data class SettledInvoice(
    val id: Long?,
    val paymentDocumentNo: Long,
    val tdsDocumentNo: Long?,
    val paymentSourceType: String?,
    val destinationId: Long?,
    val documentValue: String,
    val destinationType: SettlementType,
    val organizationId: UUID,
    val accType: String?,
    val currency: String?,
    val paymentCurrency: String,
    val tdsCurrency: String?,
    val currentBalance: BigDecimal?,
    val documentAmount: BigDecimal,
    var settledAmount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    var tds: BigDecimal? = BigDecimal.ZERO,
    val transactionDate: Date,
    var signFlag: Short,
    val settlementDate: Date,
    val exchangeRate: BigDecimal,
    var status: String?,
    var settledTds: BigDecimal
)
