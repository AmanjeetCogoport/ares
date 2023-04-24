package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class TodayPurchaseStats(
    var totalExpense: BigDecimal? = 0.toBigDecimal(),
    var totalBills: Long? = 0,
    var totalPurchaseOrgs: Long? = 0,
    var totalPurchaseCreditNotes: Long? = 0
)
