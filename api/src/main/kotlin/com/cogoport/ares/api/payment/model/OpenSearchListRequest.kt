package com.cogoport.ares.api.payment.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class OpenSearchListRequest(
    var openSearchList: List<OpenSearchList>
)
