package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID
@Introspected
@MappedEntity
data class OverallStatsForTradeParty(
    val organizationId: UUID?,
    val totalOverdueAmount: BigDecimal?,
    val totalOutstandingAmount: BigDecimal?,
    val dueByThirtyDaysAmount: BigDecimal?,
    val dueBySixtyDaysAmount: BigDecimal?,
    val dueByNinetyDaysAmount: BigDecimal?,
    val dueByNinetyPlusDaysAmount: BigDecimal?,
    val dueByThirtyDaysCount: Int?,
    val dueBySixtyDaysCount: Int?,
    val dueByNinetyDaysCount: Int?,
    val dueByNinetyPlusDaysCount: Int?,
)
