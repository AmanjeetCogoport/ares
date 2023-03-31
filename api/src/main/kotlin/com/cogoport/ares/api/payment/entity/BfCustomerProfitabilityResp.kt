package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class BfCustomerProfitabilityResp(
    var businessName: String,
    var entity: String,
    var shipmentCount: String,
    var bookedIncome: BigDecimal,
    var bookedExpense: BigDecimal,
    var profitability: BigDecimal,
)
