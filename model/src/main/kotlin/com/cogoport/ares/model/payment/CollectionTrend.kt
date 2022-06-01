package com.cogoport.ares.model.payment

import java.math.BigDecimal

data class CollectionTrend(
    var duration: String?,
    var receivableAmount: BigDecimal?,
    var collectableAmount: BigDecimal?
)
