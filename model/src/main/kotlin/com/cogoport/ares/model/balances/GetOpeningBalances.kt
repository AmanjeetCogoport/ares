package com.cogoport.ares.model.balances

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity
data class GetOpeningBalances(
    var tradePartyDetailId: UUID,
    var balanceAmount: BigDecimal,
    var ledgerCurrency: String,
)
