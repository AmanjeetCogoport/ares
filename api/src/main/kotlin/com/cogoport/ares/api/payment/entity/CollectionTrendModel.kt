package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CollectionTrendModel(
    val month: String?,
    val totalReceivableAmount: BigDecimal?,
    val totalCollectedAmount: BigDecimal?
)
