package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected

@Introspected
data class DeleteConsolidatedInvoicesReq(
    var docValues: List<String>,
    var jobId: Long,
    var performedBy: String?,
    var performedByUserType: String?
)
