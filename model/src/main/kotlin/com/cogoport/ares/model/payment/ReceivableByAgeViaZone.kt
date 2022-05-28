package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class ReceivableByAgeViaZone(
    @JsonProperty("zoneName")
    var zoneName: String?,

    @JsonProperty("ageingBucket")
    var ageingBucket: MutableList<AgeingBucket>
)
