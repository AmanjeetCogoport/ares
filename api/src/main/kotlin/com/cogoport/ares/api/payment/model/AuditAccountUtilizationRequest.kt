package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.payment.entity.AccountUtilization

data class AuditAccountUtilizationRequest(
    var accountUtilization: AccountUtilization,
    var actionName: String,
    var performedById: String?,
    var performedByType: String?
)
