package com.cogoport.ares.model.sage

import io.micronaut.core.annotation.Introspected

@Introspected
data class SageFailedResponse(
    var failedIdsList: MutableList<Long?>
)
