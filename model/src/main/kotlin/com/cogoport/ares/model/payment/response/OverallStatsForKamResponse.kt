package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class OverallStatsForKamResponse(
    val proformaInvoices: StatsForKamResponse?,
    val dueForPayment: StatsForKamResponse?,
    val overdueInvoices: StatsForKamResponse?,
    val totalReceivables: StatsForKamResponse?,
    val overDueInvoicesByDueDate: OverdueInvoicesResponse?
)
