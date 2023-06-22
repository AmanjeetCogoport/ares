package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class LSPLedgerResponse(
    val ledgerCurrency: String,
    val openingBalance: BigDecimal,
    val closingBalance: BigDecimal,
    val ledgerDocuments: List<LSPLedgerDocuments>,
    var totalPages: Long? = 0,
    var totalRecords: Long? = 0,
    var page: Int? = 0
)
