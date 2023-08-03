package com.cogoport.ares.api.dunning.model.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@MappedEntity
data class DunningPayments(
    var documentValue: String,
    var ledCurrency: String,
    var amountLoc: BigDecimal,
    var payLoc: BigDecimal,
    var transactionDate: Timestamp?,
    var signFlag: Int,
)
