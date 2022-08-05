package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class CogoEntitiesRequest(
    var entityCode: String? = null
)
