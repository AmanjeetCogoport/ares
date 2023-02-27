package com.cogoport.ares.api.common.models

import com.cogoport.ares.api.payment.entity.OverallStats
import io.micronaut.core.annotation.Introspected

@Introspected
data class OutstandingOpensearchResponse(
    var overallStats: OverallStats?,
    var outstandingServiceWise: HashMap<String, ServiceLevelOutstanding>?
)
