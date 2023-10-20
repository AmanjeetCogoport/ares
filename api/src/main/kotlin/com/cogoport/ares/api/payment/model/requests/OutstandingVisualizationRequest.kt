package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Introspected
data class OutstandingVisualizationRequest(
    var startDate: String? = LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern(AresConstants.YEAR_DATE_FORMAT)),
    var endDate: String? = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(AresConstants.YEAR_DATE_FORMAT)),
    var bifurcationType: String? = "overall",
    var entityCode: List<Int>? = listOf(101, 301),
    var periodType: String? = "week",
    var kamOwnerId: UUID? = null,
    var viewType: String? = "outstanding",
    var page: Int? = 1,
    var pageLimit: Int? = 6
)
