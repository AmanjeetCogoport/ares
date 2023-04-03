package com.cogoport.ares.api.payment.model.requests

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected

@Introspected
data class ServiceWiseRecPayReq(
    var entityCode: MutableList<Int>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String?,
)