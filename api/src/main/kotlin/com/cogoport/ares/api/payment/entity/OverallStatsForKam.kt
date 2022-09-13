package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverallStatsForKam(
    val TotalAmount: BigDecimal?,
    val InvoicesCount: Int?,
    val CustomersCount: Int?
)
