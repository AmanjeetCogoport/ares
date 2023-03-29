package com.cogoport.ares.api.sage.service.interfaces

import com.cogoport.ares.model.payment.AccountType

interface SageService {
    suspend fun checkIfDocumentExistInSage(documentValue: String, sageBPRNumber: String, organizationSerialId: Long?, documentType: AccountType, registrationNumber: String?): Boolean
}
