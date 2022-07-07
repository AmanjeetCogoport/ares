package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected

@Introspected
data class CogoEntitiesRequest(
    var authToken: String,
    var entityCode: Int
)
