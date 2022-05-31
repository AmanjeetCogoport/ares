package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class OutstandingListRequest(
    @QueryValue(AresConstants.ZONE) val zone: String? = null,
    @QueryValue(AresConstants.ROLE) val role: String? = null,
    @QueryValue(AresConstants.ORG_NAME) val orgName: String? = "",
    @QueryValue(AresConstants.PAGE) val page: Int = 1,
    @QueryValue(AresConstants.PAGE_LIMIT) val pageLimit: Int = 10
)
