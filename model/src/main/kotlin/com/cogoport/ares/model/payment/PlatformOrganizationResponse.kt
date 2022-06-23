package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlatformOrganizationResponse(

    @JsonProperty("list")
    val list: List<PlatformOrganizationDetials>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("total")
    val total: Int,

    @JsonProperty("total_count")
    val totalCount: Int,

    @JsonProperty("page_limit")
    val pageLimit: Int
)
