package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfTodayStatReq(
    var serviceTypes: List<ServiceType>? = null,
    var entityCode: Int? = null,
)
