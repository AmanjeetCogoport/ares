package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected

@Introspected

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class InvoiceBalanceEvent(
    var invoiceBalance: InvoiceBalance
)
