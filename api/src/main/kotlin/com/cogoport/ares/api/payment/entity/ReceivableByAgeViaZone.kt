package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected

@Introspected
data class ReceivableByAgeViaZone(
    var zoneName: String?,
    var ageingBucket: MutableList<com.cogoport.ares.api.payment.model.AgeingBucket>
)
