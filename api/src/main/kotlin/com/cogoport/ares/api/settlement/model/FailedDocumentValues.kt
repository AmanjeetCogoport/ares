package com.cogoport.ares.api.settlement.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class FailedDocumentValues(
    var failedDocumentValues: List<String>?
)
