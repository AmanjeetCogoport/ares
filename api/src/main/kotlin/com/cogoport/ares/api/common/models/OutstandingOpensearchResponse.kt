package com.cogoport.ares.api.common.models

import com.cogoport.ares.api.payment.entity.OverallStats
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class OutstandingOpensearchResponse(
    @JsonProperty("overallStats")
    var overallStats: OverallStats?,
    @JsonProperty("outstandingServiceWise")
    var outstandingServiceWise: HashMap<String, ServiceLevelOutstanding>?
)
