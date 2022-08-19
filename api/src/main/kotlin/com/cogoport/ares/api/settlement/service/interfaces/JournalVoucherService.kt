package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JournalVoucherApproval
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import java.util.UUID

interface JournalVoucherService {

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse>

    suspend fun createJournalVoucher(request: JournalVoucherRequest): String

    suspend fun approveJournalVoucher(request: JournalVoucherApproval)

    suspend fun rejectJournalVoucher(id: Long, performedBy: UUID?)
}
