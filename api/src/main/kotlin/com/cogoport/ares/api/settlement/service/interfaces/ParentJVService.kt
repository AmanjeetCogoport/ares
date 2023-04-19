package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.GlCode
import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
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

    suspend fun getJvCategory(q: String?, pageLimit: Int?): List<JvCategory>

    suspend fun getGLCode(entityCode: Int?, q: String?, pageLimit: Int?): List<GlCode>

    suspend fun getGLCodeMaster(accMode: AccMode?, q: String?, pageLimit: Int?, entityCode: Int?): List<GlCodeMaster>

    suspend fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode>
}
