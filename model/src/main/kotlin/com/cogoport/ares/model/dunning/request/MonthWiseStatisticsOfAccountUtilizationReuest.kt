package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class MonthWiseStatisticsOfAccountUtilizationReuest(
    var viewType: String? = "FY",
    var serviceType: String?,
    var year: Int?
)
