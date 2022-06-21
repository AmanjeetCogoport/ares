package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
@Introspected
data class OrganizationReceivablesRequest(
    @QueryValue(AresModelConstants.ORG_ID) val orgId: String? = null,
    @QueryValue(AresModelConstants.YEAR) val year: Int = AresModelConstants.CURR_YEAR,
    @QueryValue(AresModelConstants.MONTH) val month: Int = AresModelConstants.CURR_MONTH,
    @QueryValue(AresModelConstants.COUNT) val count: Int = 4
)
