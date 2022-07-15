package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.KnockOffStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettledDocument(
    val id: Long?,
    var documentNo: Long,
    val sid: String?,
    val amount: BigDecimal,
    val amountLedger: BigDecimal,
    val currentBalance: BigDecimal,
    val accType: SettlementType,
    val transactionDate: Date,
    val settlementStatus: KnockOffStatus
)
