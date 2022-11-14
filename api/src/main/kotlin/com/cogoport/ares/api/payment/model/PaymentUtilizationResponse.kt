package com.cogoport.ares.api.payment.model

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class PaymentUtilizationResponse(
    var id:Long,
    var payCurr: BigDecimal,
    var payLoc: BigDecimal
)
