package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class AutoKnockoffDocumentResponse(
    var accountUtilization: AccountUtilization? = null,
    var paidTds: BigDecimal? = null,
    var payableTds: BigDecimal? = null,
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    var amount: BigDecimal = BigDecimal.ZERO
)
