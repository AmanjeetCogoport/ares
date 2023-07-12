package com.cogoport.ares.model.payment.response

import java.math.BigDecimal

data class LedgerReportHeader(
    val cogoEntity: List<Int>,
    val companyName: String,
    val startDate: String,
    val endDate: String,
    val ledgerCurrency: String,
    val documentDate: String,
    val openingBalance: BigDecimal,
    val closingBalance: BigDecimal
)
