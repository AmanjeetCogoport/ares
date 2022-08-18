package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest

interface JournalVoucherService {

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse>

    suspend fun createJournalVouchers(request: JournalVoucherRequest): String
}
