package com.cogoport.ares.model.common

import com.cogoport.ares.model.dunning.response.OrganizationTradePartyDetails
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
data class GetOrganizationTradePartyDetailResponse(

    @JsonProperty("list")
    val list: List<OrganizationTradePartyDetails>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("total")
    val total: Int,

    @JsonProperty("total_count")
    val totalCount: Int,

    @JsonProperty("page_limit")
    val pageLimit: Int
)
