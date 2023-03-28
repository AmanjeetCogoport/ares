package com.cogoport.ares.api.payment.model.requests

import com.fasterxml.jackson.annotation.JsonFormat

data class serviceWiseRecPayReq(
    var entityCode: Int? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String?,
)
