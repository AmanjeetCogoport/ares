package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import io.micronaut.http.annotation.QueryValue

data class SettlementHistoryRequest(
    @QueryValue(AresModelConstants.ORG_ID) val orgId: List<String>? = null,
    @QueryValue(AresModelConstants.SETTLEMENT_TYPE) val settType: String? = null,
    @QueryValue(AresModelConstants.START_DATE) val startDate: String? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: String? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACC_MODE) val accMode: AccMode? = null
)
