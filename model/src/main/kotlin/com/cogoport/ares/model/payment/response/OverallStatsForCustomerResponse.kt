package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverallStatsForCustomerResponse(
    var custId: String?,
    var kamProformaCount:DueCountResponse?,
    var proformaInvoices: StatsForCustomerResponse?,
    var dueForPayment: StatsForCustomerResponse?,
    var overdueInvoices: StatsForCustomerResponse?,
    var totalReceivables: StatsForCustomerResponse?,
    var onAccountPayment: BigDecimal?,
    var overDueInvoicesByDueDate: OverdueInvoicesResponse?
)
