package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class AutoKnockoffDocumentResponse(
    var accountUtilization: AccountUtilization? = null,
    var settlementId: Long? = null,
    var paidTds: BigDecimal? = null,
    var payableTds: BigDecimal? = null,
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    val isSettled: Boolean? = false,
    var amount: BigDecimal = BigDecimal.ZERO,
    var tdsAmount: BigDecimal = BigDecimal.ZERO,
    var taggedSettledIds: MutableList<Long?>? = mutableListOf()
)
