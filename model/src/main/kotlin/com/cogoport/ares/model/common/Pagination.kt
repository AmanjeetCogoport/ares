package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.Positive

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
open class Pagination() {
    @JsonProperty("pageSize")
    @Positive(message = "Index must be positive digit")
    var pageSize: Int = 10

    @JsonProperty("pageIndex")
    @Positive(message = "Index must be positive digit")
    var pageIndex: Int = 1
}
