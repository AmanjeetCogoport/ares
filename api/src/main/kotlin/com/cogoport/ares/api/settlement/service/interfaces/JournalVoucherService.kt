package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.model.JournalVoucherApproval
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherReject
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import java.util.UUID

interface JournalVoucherService {

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse>

    suspend fun createJournalVoucher(request: JournalVoucherRequest): String

    suspend fun approveJournalVoucher(request: JournalVoucherApproval): String

    suspend fun rejectJournalVoucher(request: JournalVoucherReject): String

    suspend fun updateJournalVoucherStatus(id: Long, status: JVStatus, performedBy: UUID, performedByUserType: String?)
    suspend fun postJVToSage(jvId: Long, performedBy: UUID): Boolean
    suspend fun createJV(jv: JournalVoucher): JournalVoucher
    suspend fun sendToIncidentManagement(request: JournalVoucherRequest, data: com.cogoport.hades.model.incident.JournalVoucher)
    suspend fun createJvAccUtil(request: JournalVoucher, accMode: AccMode, signFlag: Short): AccountUtilization
}
