package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected

@Introspected
data class InvoiceTatStatsResponse(
    var draftInvoicesCount: Int? = 0,
    var financeAcceptedInvoiceCount: Int? = 0,
    var irnGeneratedInvoicesCount: Int? = 0,
    var settledInvoicesCount: Int? = 0,
    var tatHoursFromDraftToFinanceAccepted: Long? = 0,
    var tatHoursFromFinanceAcceptedToIrnGenerated: Long? = 0,
    var tatHoursFromIrnGeneratedToSettled: Long? = 0,
)
