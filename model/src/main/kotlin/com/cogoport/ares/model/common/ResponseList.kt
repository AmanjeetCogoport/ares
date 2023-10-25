package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Nullable

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseList<T>(
    @Nullable
    var list: List<T?> = listOf(),
    var totalPages: Long? = 0,
    var totalRecords: Long? = 0,
    var pageNo: Int? = 0
) {
    @javax.persistence.Transient
    var byCallPriority: T? = null
}
