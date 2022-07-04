package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.AuditAccountUtilizationRequest
import com.cogoport.ares.api.payment.model.AuditPaymentRequest

interface AuditService {
    suspend fun auditPayment(request: AuditPaymentRequest)

    suspend fun auditAccountUtilization(request: AuditAccountUtilizationRequest)
}