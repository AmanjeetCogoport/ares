package com.cogoport.ares.model.common

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class TradePartyOutstandingRes(
    val openInvoicesLedAmount: BigDecimal,
    val overdueOpenInvoicesLedAmount: BigDecimal,
    val openInvoicesCount: Int,
    val outstandingLedAmount: BigDecimal
)
