package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class TodaySalesStat(
    var totalRevenue: BigDecimal? = 0.toBigDecimal(),
    var totalInvoices: Long? = 0,
    var totalSalesOrgs: Long? = 0,
    var totalSalesCreditNotes: Long? = 0
)
