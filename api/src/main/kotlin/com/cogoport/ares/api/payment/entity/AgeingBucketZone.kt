package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import software.amazon.awssdk.services.servicediscovery.model.ServiceType
import java.math.BigDecimal
import java.util.Currency
import javax.persistence.Enumerated

@MappedEntity
data class AgeingBucketZone(
    val ageingDuration: String,
    val amount: BigDecimal,
    val zone: String,
    val serviceType: String?,
    val currency: String?
)
