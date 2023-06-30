package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotNull

@Introspected
data class BfPendingAmountsReq(
    var serviceTypes: List<ServiceType>? = null,
    @NotNull
    var accountMode: AccMode? = AccMode.AR,
    var buyerType: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String? = null,
    var tradeType: List<String>? = null,
    var entityCode: MutableList<Int>? = mutableListOf(101, 301)
)
