package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class Statistics(
        var organizationId: String,
        var todayAmount: BigDecimal,
        var sevenAmount: BigDecimal,
        var dueTillTodayAmount: BigDecimal,
        var dueBeforeTodayAmount: BigDecimal,
        var fifteenAmount: BigDecimal,
        var monthAmount: BigDecimal,
        var threeMonthAmount: BigDecimal,
        var sixMonthAmount: BigDecimal,
        var todayCount: Int,
        var dueTillTodayCount: Int,
        var dueBeforeTodayCount: Int,
        var sevenCount: Int,
        var fifteenCount: Int,
        var monthCount: Int,
        var threeMonthCount: Int,
        var sixMonthCount: Int,
        var customAmount: BigDecimal,
        var customCount: Int
)
