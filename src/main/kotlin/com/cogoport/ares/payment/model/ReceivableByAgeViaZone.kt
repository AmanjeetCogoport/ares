package com.cogoport.ares.payment.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class ReceivableByAgeViaZone(
    var zoneName: String?,
    var ageingBucket: MutableList<AgeingBucket>
)
