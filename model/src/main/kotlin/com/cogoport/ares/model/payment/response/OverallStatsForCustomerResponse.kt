package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverallStatsForCustomerResponse(
    val proformaInvoices: StatsForCustomerResponse?,
    val dueForPayment: StatsForCustomerResponse?,
    val overdueInvoices: StatsForCustomerResponse?,
    val totalReceivables: StatsForCustomerResponse?,
    val onAccountPayment: BigDecimal?,
    val overDueInvoicesByDueDate: OverdueInvoicesResponse?
)
