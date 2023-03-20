package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@MappedEntity
data class Document(
    var id: Long,
    var sourceId: Long?,
    var sourceType: SettlementType?,
    var settledTds: BigDecimal,
    var tdsCurrency: String?,
    var documentNo: Long,
    var organizationId: UUID,
    var mappingId: UUID?,
    var documentValue: String,
    var documentType: String,
    var accountType: String,
    var documentDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var documentLedAmount: BigDecimal,
    var documentLedBalance: BigDecimal,
    var taxableAmount: BigDecimal,
    var afterTdsAmount: BigDecimal,
    var settledAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var currency: String,
    var ledCurrency: String,
    var exchangeRate: BigDecimal,
    var approved: Boolean?,
    var signFlag: Short,
    var accMode: AccMode,
    var tds: BigDecimal = BigDecimal.ZERO
)
