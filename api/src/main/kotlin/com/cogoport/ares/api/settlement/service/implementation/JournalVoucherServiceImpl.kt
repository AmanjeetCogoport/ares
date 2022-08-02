package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.model.settlement.request.JvListRequest
import jakarta.inject.Singleton

@Singleton
class JournalVoucherServiceImpl: JournalVoucherService {
    override suspend fun getJournalVouchers(jvListRequest: JvListRequest) {
        TODO("Not yet implemented")
    }
}