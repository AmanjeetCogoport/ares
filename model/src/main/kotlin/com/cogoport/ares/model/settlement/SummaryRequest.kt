package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SummaryRequest(
    @QueryValue(AresModelConstants.ENTITY_CODE) val entityCode: String? = null,
    @QueryValue(AresModelConstants.ORG_ID) val orgId: List<String>? = null,
    @QueryValue(AresModelConstants.START_DATE) val startDate: String? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: String? = null
)
