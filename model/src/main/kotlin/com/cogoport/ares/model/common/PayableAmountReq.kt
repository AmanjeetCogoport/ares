package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class PayableAmountReq(
    val documentNo: Long,
    val tdsAmount: BigDecimal,
    val tdsAmountLoc: BigDecimal
)
