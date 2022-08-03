package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.model.settlement.JournalVoucher
import com.cogoport.ares.model.settlement.request.JvListRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JournalVoucherServiceImpl: JournalVoucherService {

    @Inject

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucher> {
        TODO("Not yet implemented")
    }

    override suspend fun createJournalVouchers(journalVoucher: JournalVoucher) {

        TODO("Not yet implemented")
    }
}