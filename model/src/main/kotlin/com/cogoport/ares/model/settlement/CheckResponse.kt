package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected

@Introspected
data class CheckResponse(
    var stackDetails: List<CheckDocument>,
    var canSettle: Boolean
)
