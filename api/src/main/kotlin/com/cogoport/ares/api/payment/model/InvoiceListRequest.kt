package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.http.annotation.QueryValue

data class InvoiceListRequest(
    @QueryValue(AresConstants.ZONE) val zone: String?,
    @QueryValue(AresConstants.ORG_ID) val orgId: String?,
    @QueryValue(AresConstants.PAGE) val page: Int = 1,
    @QueryValue(AresConstants.PAGE_LIMIT) val pageLimit: Int = 20
)
