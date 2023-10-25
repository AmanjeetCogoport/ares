package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected

@Introspected
data class SalesFunnelResponse(
    var draftInvoicesCount: Int? = 0,
    var financeAcceptedInvoiceCount: Int? = 0,
    var irnGeneratedInvoicesCount: Int? = 0,
    var settledInvoicesCount: Int? = 0,
    var draftToFinanceAcceptedPercentage: Int? = 0,
    var financeToIrnPercentage: Int? = 0,
    var settledPercentage: Int? = 0,
    var irnFailedInvoicesCount: Int? = 0,
    var financeToIrnFailedPercentage: Int? = 0,
    var irnCancelledInvoicesCount: Int? = 0,
    var irnGenToIrnCancelledPercentage: Int? = 0
)
