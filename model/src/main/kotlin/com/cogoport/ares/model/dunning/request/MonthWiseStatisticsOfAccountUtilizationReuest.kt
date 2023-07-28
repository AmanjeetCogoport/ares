package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class MonthWiseStatisticsOfAccountUtilizationReuest(
    var viewType: String? = "FY",
    var serviceTypes: List<String>?,
    var entityCodes: List<Int>?,
    var year: Int?
)
