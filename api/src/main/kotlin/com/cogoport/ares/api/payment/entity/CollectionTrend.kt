package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CollectionTrend(
    var duration: String?,
    var receivableAmount: BigDecimal?,
    var collectableAmount: BigDecimal?,
    var currencyType: String?,
    var serviceType: String?,
    var invoiceCurrency: String?
)
