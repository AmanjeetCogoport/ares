package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.model.AuditAccountUtilizationRequest
import com.cogoport.ares.api.payment.model.AuditPaymentRequest
import com.cogoport.ares.api.payment.repository.AuditRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@Singleton
class AuditServiceImpl : AuditService {
    @Inject
    lateinit var auditRepository: AuditRepository

    override suspend fun auditPayment(request: AuditPaymentRequest) {
        saveToAudits(
            objectType = AresConstants.PAYMENTS, objectId = request.payment.id!!, actionName = request.actionName, data = request.payment, performedById = request.performedById, performedByType = request.performedByType
        )
    }

    override suspend fun auditAccountUtilization(request: AuditAccountUtilizationRequest) {
        saveToAudits(
            objectType = AresConstants.ACCOUNT_UTILIZATIONS, objectId = request.accountUtilization.id!!, actionName = request.actionName, data = request.accountUtilization, performedById = request.performedById, performedByType = request.performedByType
        )
    }

    private suspend fun saveToAudits(objectType: String, objectId: Long, actionName: String, data: Any, performedById: String?, performedByType: String?) {
        val id: UUID? = if (performedById != null) UUID.fromString(performedById) else null
        auditRepository.save(
            Audit(
                id = null,
                objectType = objectType,
                objectId = objectId,
                actionName = actionName,
                data = data,
                performedBy = id,
                performedByUserType = performedByType,
                createdAt = Timestamp.valueOf(LocalDateTime.now())
            )
        )
    }
}
