package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
@MappedEntity
data class SummaryResponse(
    val amount: BigDecimal,
    val ledgerCurrency: String
)
