package com.cogoport.ares.api.payment.model

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class BillPaymentSumResponse(
    var amountSum: BigDecimal,
    var ledgerAmountSum: BigDecimal
)
