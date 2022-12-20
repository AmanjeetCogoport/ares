package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit

interface ThirdPartyApiAuditService {

    suspend fun createAudit(request: ThirdPartyApiAudit): Boolean
}