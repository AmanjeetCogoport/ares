package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class JvListRequest(
    @QueryValue(AresModelConstants.START_DATE) val startDate: String? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: String? = null,
    @QueryValue(AresModelConstants.STATUS) val status: JVStatus? = null,
    @QueryValue(AresModelConstants.CATEGORY) val category: String? = null,
    @QueryValue(AresModelConstants.TYPE) val type: String? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = "",
    @QueryValue("entityCode") var entityCode: Int? = null,
    @QueryValue("sortBy") val sortBy: String? = "createdAt",
    @QueryValue("sortType") val sortType: String? = "Desc"
)
