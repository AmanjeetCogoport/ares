package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementHistoryRequest(
    @QueryValue(AresModelConstants.ORG_ID) val orgId: List<UUID>,
    @QueryValue(AresModelConstants.ACCOUNT_TYPE) val accountType: String,
    @QueryValue(AresModelConstants.START_DATE) val startDate: String? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: String? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACC_MODE) val accMode: AccMode? = null
)
