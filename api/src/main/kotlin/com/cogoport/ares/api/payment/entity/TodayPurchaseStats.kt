package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class TodayPurchaseStats(
    var totalExpense: BigDecimal,
    var totalBills: Long,
    var totalPurchaseOrgs: Long
)
