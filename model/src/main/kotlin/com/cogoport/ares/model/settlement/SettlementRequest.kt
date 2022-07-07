package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccountType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementRequest(
    @QueryValue(AresModelConstants.DOCUMENT_NO) val documentNo: Long,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACCOUNT_TYPE) val accType: AccountType? = null
)
