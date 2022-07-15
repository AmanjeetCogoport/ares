package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class TdsStylesRequest(
    @QueryValue("id") var id: String
)
