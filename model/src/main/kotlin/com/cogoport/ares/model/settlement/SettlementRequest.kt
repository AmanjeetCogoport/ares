package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementRequest(
    @QueryValue(AresModelConstants.DOCUMENT_NO) var documentNo: String,
    @QueryValue(AresModelConstants.SETTLEMENT_TYPE) val settlementType: SettlementType,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null
)
