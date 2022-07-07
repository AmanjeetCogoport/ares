package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonInclude
import reactor.util.annotation.Nullable

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseList<T>(
    @Nullable
    var list: List<T>? = listOf(),
    var totalPages: Long? = 0,
    var totalRecords: Long? = 0,
    var pageNo: Int? = 0
)
