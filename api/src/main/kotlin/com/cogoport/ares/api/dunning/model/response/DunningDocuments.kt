package com.cogoport.ares.api.dunning.model.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity
data class DunningDocuments(
    var documentNo: Long,
    var documentValue: String,
    var ledCurrency: String,
    var amountLoc: BigDecimal,
    var payLoc: BigDecimal,
    var dueDate: Date,
    var invoiceType: String,
    var relativeDuration: String,
    var transactionDate: Date,
    var signFlag: Long
)
