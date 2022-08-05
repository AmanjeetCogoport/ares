package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementInvoiceRequest(
    @QueryValue(AresModelConstants.ORG_ID) val orgId: List<UUID> = emptyList(),
    @QueryValue(AresModelConstants.STATUS) val status: DocStatus? = null,
    @QueryValue(AresModelConstants.ENTITY_CODE) val entityCode: Int? = null,
    @QueryValue(AresModelConstants.START_DATE) val startDate: Timestamp? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: Timestamp? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACCOUNT_TYPE) var accType: AccountType? = null
)
