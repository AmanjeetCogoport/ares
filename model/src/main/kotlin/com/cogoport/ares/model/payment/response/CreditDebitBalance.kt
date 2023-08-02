package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class CreditDebitBalance(
    var ledgerCurrency: String?,
    var debit: BigDecimal,
    var credit: BigDecimal
)
