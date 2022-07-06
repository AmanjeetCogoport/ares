package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccountType
import io.micronaut.http.annotation.QueryValue

data class SettlementRequest(
    @QueryValue(AresModelConstants.DOCUMENT_NO) val documentNo: String,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACCOUNT_TYPE) val accType: AccountType? = null
)
