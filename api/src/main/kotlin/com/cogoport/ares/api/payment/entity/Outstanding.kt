package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class Outstanding(
    var duration: String?,
    var amount: BigDecimal,
    var currencyType: String?,
    var serviceType: ServiceType?
)
