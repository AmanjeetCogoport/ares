package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected

@Introspected
data class ReceivableByAgeViaZone(
    var zoneName: String?,
    var ageingBucket: MutableList<AgeingBucket>
)
