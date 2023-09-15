package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.model.AuditRequest
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
    override suspend fun createAudit(request: AuditRequest) {
        var performedById: UUID? = null
        try {
            performedById = UUID.fromString(request.performedBy)
        } catch (_: Exception) {}
        auditRepository.save(
            Audit(
                id = null,
                objectType = request.objectType,
                objectId = request.objectId,
                actionName = request.actionName,
                data = request.data,
                performedBy = performedById,
                performedByUserType = request.performedByUserType,
                createdAt = Timestamp.valueOf(LocalDateTime.now())
            )
        )
    }

    override suspend fun createAudits(requests: List<AuditRequest>) {
        val audits: MutableList<Audit> = ArrayList()
        requests.forEach { request ->
            var performedById: UUID? = null
            try {
                performedById = UUID.fromString(request.performedBy)
            } catch (_: Exception) {}
            audits.add(
                Audit(
                    id = null,
                    objectType = request.objectType,
                    objectId = request.objectId,
                    actionName = request.actionName,
                    data = request.data,
                    performedBy = performedById,
                    performedByUserType = request.performedByUserType,
                    createdAt = Timestamp.valueOf(LocalDateTime.now())
                )
            )
        }
        auditRepository.saveAll(audits)
    }
}
