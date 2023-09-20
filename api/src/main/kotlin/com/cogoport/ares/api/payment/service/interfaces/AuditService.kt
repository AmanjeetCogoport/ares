package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.AuditRequest

interface AuditService {
    suspend fun createAudit(request: AuditRequest)

    suspend fun createAudits(requests: List<AuditRequest>)
}
