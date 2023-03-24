package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class TodaySalesStat(
    var totalRevenue: BigDecimal,
    var totalInvoices: Long,
    var totalSalesOrgs: Long
)
