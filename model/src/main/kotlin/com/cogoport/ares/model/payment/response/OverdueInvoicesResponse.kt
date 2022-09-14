package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverdueInvoicesResponse(
    val thirtyAmount: BigDecimal?,
    val sixtyAmount: BigDecimal?,
    val ninetyAmount: BigDecimal?,
    val ninetyPlusAmount: BigDecimal?,
    val thirtyCount: Int?,
    val sixtyCount: Int?,
    val ninetyCount: Int?,
    val ninetyPlusCount: Int?
)
