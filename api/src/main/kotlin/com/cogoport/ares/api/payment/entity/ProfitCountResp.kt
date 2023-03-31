package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class ProfitCountResp(
    var totalCount: Long? = 0,
    var averageProfit: BigDecimal? = 0.toBigDecimal()
)
