package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonInclude
data class ListOrganizationTradePartyDetailsResponse(
    val list: ArrayList<HashMap<String, Any?>>,
    val page: Int? = 1,
    val total: Int? = 1,
    val totalCount: Int? = 1,
    val pageLimit: Int? = 10
)
