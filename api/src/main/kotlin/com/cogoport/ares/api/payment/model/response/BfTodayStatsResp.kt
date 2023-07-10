package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.api.payment.entity.TodayPurchaseStats
import com.cogoport.ares.api.payment.entity.TodaySalesStat
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class BfTodayStatsResp(
    var todaySalesStats: TodaySalesStat,
    var todayPurchaseStats: TodayPurchaseStats
) {
    @field:javax.persistence.Transient
    var totalCashFlow: BigDecimal? = null

    @field:javax.persistence.Transient
    var yesterdayCashFlow: BigDecimal? = null

    @field:javax.persistence.Transient
    var cashFlowDiffFromYesterday: BigDecimal? = null

    @field:javax.persistence.Transient
    var currency: String? = "INR"
}
