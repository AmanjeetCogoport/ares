package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected

@Introspected
data class ListOrgStylesRequest(
    val ids: List<String>
)
