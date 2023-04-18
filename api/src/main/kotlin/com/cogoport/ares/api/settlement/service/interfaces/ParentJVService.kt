package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJVUpdateRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import java.util.UUID

interface ParentJVService {
    suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String?

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse>

    suspend fun updateParentJv(request: ParentJVUpdateRequest): String

    suspend fun deleteJournalVoucherById(id: String, performedBy: UUID): String

    suspend fun editJv(request: ParentJournalVoucherRequest): String

    suspend fun postJVToSage(parentJVId: Long, performedBy: UUID): Boolean
}
