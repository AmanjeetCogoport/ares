package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import software.amazon.awssdk.services.servicediscovery.model.ServiceType
import java.math.BigDecimal

@MappedEntity
data class CollectionTrend(
    var duration: String?,
    var receivableAmount: BigDecimal?,
    var collectableAmount: BigDecimal?,
    var currency: String?,
    var serviceType: String?
)
