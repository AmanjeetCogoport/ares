package com.cogoport.ares.model.settlement.event

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateInvoiceBalanceEvent(
    var invoiceBalance: InvoiceBalance,
    var knockoffDocuments: List<PaymentInvoiceInfo>? = null
)
