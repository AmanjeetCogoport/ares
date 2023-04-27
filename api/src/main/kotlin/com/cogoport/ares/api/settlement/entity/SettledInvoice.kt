package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@MappedEntity
data class SettledInvoice(
    val id: Long?,
    val paymentDocumentNo: Long,
    val tdsDocumentNo: Long?,
    val tdsType: SettlementType?,
    val destinationId: Long?,
    val documentValue: String,
    val destinationType: SettlementType,
    val organizationId: UUID,
    val accType: String?,
    val currency: String?,
    val paymentCurrency: String,
    val tdsCurrency: String?,
    val currentBalance: BigDecimal,
    val documentAmount: BigDecimal,
    var settledAmount: BigDecimal,
    val ledCurrency: String,
    var ledAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    var tds: BigDecimal,
    val transactionDate: Date?,
    var signFlag: Short,
    val exchangeRate: BigDecimal,
    var settledTds: BigDecimal,
    var nostroAmount: BigDecimal,
    var accMode: AccMode,
    var settlementStatus: SettlementStatus
)
