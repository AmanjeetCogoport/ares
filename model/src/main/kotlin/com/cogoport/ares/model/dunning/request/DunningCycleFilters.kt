package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DunningCycleFilters(
    val cogoEntityId: UUID?,
    var serviceType: List<ServiceType>?,
    val creditController: List<UUID>?,
    val ageingBucket: AgeingBucketEnum?,
    val totalDueOutstanding: BigDecimal?,
    val dueOutstandingCurrency: String?
)
