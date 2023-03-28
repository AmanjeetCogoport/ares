package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotNull

@Introspected
data class BfPendingAmountsReq(
    var serviceType: List<ServiceType>?,
    @NotNull
    var accountMode: AccMode,
    var buyerType: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String?,
    var tradeType: List<String>?,
    var entityCode: Int?
)
