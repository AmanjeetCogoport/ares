package com.cogoport.ares.api.payment.model

data class AuditRequest(
    var objectType: String,
    var objectId: Long?,
    var actionName: String,
    var data: Any?,
    var performedBy: String?,
    var performedByUserType: String?
)
