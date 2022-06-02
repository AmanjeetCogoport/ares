package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
@Introspected
data class InvoiceListRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String? = null,
    @QueryValue(AresModelConstants.ORG_ID) val orgId: String? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 20
)
