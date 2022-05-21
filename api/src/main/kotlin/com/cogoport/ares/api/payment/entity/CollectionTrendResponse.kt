package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CollectionTrendResponse(
    var duration: String?,
    var receivableAmount: BigDecimal?,
    var collectableAmount: BigDecimal?,
)
