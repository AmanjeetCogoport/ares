package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DunningCycleFilters(
    @JsonProperty("cogoEntityId")
    @NotNull("entity id can not be null.")
    val cogoEntityId: UUID? = null,
    @JsonProperty("serviceTypes")
    var serviceTypes: List<ServiceType>? = null,
    @JsonProperty("creditControllerIds")
    val creditControllerIds: List<UUID>? = null,
    @JsonProperty("ageingBucket")
    val ageingBucket: String? = AgeingBucketEnum.ALL.toString(),
    @JsonProperty("totalDueOutstanding")
    val totalDueOutstanding: BigDecimal? = null,
    @JsonProperty("dueOutstandingCurrency")
    val dueOutstandingCurrency: String? = null
)
