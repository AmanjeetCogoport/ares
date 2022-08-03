package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucher
import com.cogoport.ares.model.settlement.request.JvListRequest

interface JournalVoucherService {

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucher>

    suspend fun createJournalVouchers(journalVoucher: JournalVoucher)

}
