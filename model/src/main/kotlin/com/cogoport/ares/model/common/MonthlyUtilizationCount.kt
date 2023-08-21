package com.cogoport.ares.model.common

import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class MonthlyUtilizationCount(
    var lastMonth: Int,
    var secondLastMonth: Int,
    var thirdLastMonth: Int,
    var fourthLastMonth: Int,
    var fifthLastMonth: Int,
    var sixthLastMonth: Int
)
