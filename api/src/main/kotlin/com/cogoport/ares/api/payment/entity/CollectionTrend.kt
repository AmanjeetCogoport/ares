package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CollectionTrend (
    var totalReceivableAmount: BigDecimal?,
    var totalCollectedAmount: BigDecimal?,
    var collectionTrend: MutableList<CollectionTrendResponse>,
    var id: String
)