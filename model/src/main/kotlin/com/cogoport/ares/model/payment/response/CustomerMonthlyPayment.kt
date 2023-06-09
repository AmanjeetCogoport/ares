package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class CustomerMonthlyPayment(
    val ledgerCurrency: String? = null,
    var january: BigDecimal?,
    var february: BigDecimal?,
    var march: BigDecimal?,
    var april: BigDecimal?,
    var may: BigDecimal?,
    var june: BigDecimal?,
    var july: BigDecimal?,
    var august: BigDecimal?,
    var september: BigDecimal?,
    var october: BigDecimal?,
    var november: BigDecimal?,
    var december: BigDecimal?
)
