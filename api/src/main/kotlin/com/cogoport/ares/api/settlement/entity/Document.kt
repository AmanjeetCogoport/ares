package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity
data class Document(
    var id: Long,
    var documentNo: Long,
    var documentValue: String,
    var documentType: String,
    var documentDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var documentLedAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var afterTdsAmount: BigDecimal,
    var settledAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var status: String?,
    var currency: String,
    var ledCurrency: String,
    var exchangeRate: BigDecimal

)
