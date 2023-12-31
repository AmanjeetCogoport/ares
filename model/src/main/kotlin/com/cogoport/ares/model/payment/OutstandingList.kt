package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class OutstandingList(
    @JsonProperty("list")
    val list: List<CustomerOutstanding?>? = null,
    @JsonProperty("totalPage")
    val totalPage: Int,
    @JsonProperty("totalRecords")
    val totalRecords: Int,
    @JsonProperty("page")
    val page: Int
)
