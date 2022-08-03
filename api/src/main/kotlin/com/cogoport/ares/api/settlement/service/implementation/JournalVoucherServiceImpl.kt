package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherMapper: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {
        TODO()
    }

    override suspend fun createJournalVouchers(journalVoucher: JournalVoucher) {

        TODO("Not Yet implemented")
    }
}
