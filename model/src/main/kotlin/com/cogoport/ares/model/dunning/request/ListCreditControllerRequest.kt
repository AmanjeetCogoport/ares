package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class ListCreditControllerRequest(
    var query: String?
)
