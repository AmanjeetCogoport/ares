package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementDocumentRequest(
    @QueryValue(AresModelConstants.ORG_ID) val orgId: UUID,
    @QueryValue(AresModelConstants.ENTITY_CODE) var entityCode: Int? = null,
    @QueryValue(AresModelConstants.START_DATE) val startDate: Timestamp? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: Timestamp? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = "",
    @QueryValue(AresModelConstants.ACC_MODE) val accModes: List<AccMode>?,
    val docType: String? = null,
    val documentPaymentStatus: String? = null,
    val sortBy: String? = "transactionDate",
    val sortType: String? = "Desc"
)
