package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import software.amazon.awssdk.services.servicediscovery.model.ServiceType
import java.math.BigDecimal

@MappedEntity
data class CollectionTrend(
    var duration: String?,
    var receivableAmount: BigDecimal?,
    var collectableAmount: BigDecimal?,
    var currencyType: String?,
    var serviceType: String?,
    var invoiceCurrency: String?
)
