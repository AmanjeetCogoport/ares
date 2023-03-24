package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import java.util.Date
import javax.validation.constraints.NotNull

@Introspected
data class BfPendingAmountsReq(
    var serviceType: List<ServiceType>?,
    @NotNull
    var accountMode: AccMode,
    var buyerType: String?,
    var startDate: Date?,
    var endDate: Date?,
    var tradeType: String?,
    var entityCode: Int?
)
