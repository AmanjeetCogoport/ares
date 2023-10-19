package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.repository.ThirdPartyApiAuditRepository
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ThirdPartyApiAuditImpl : ThirdPartyApiAuditService {
    @Inject
    lateinit var thirdPartyApiAuditRepository: ThirdPartyApiAuditRepository

    override suspend fun createAudit(request: ThirdPartyApiAudit): Boolean {

        thirdPartyApiAuditRepository.save(
            ThirdPartyApiAudit(
                id = null,
                apiName = request.apiName,
                apiType = request.apiType,
                objectId = request.objectId,
                objectName = request.objectName,
                httpResponseCode = request.httpResponseCode,
                requestParams = request.requestParams,
                response = request.response,
                isSuccess = request.isSuccess,
                createdAt = null,
                updatedAt = null
            )
        )
        return true
    }
}
