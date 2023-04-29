package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected

@Introspected
data class FailedSettlementIds(
    var failedSettlementIds: List<Long>?
)
