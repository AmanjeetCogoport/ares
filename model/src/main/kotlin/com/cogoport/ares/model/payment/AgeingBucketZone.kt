package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AgeingBucketZone(
    var ageingDuration: String?,
    var amount: BigDecimal?,
    val zone: String? = null,
    val serviceType: ServiceType? = null,
    val currencyType: String?,
)
