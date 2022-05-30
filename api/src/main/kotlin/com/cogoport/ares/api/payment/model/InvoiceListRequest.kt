package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.http.annotation.QueryValue

data class InvoiceListRequest(
    @QueryValue(AresConstants.ZONE) val zone: String?,
    @JsonProperty(AresConstants.ORG_ID)
    @QueryValue(AresConstants.ORG_ID) val orgId: String?,
    @QueryValue(AresConstants.PAGE) val page: Int = 1,
    @JsonProperty(AresConstants.PAGE_LIMIT)
    @QueryValue(AresConstants.PAGE_LIMIT) val pageLimit: Int = 20
)
