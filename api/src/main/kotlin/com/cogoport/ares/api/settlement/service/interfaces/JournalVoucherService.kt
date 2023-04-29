package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JvLineItemResponse
import java.util.UUID

interface JournalVoucherService {

    suspend fun updateJournalVoucherStatus(id: Long, isUtilized: Boolean, performedBy: UUID, performedByUserType: String?, documentValue: String?)

    suspend fun createJvAccUtil(request: JournalVoucher, accMode: AccMode, signFlag: Short): AccountUtilization

    suspend fun getJVLineItems(parentJVId: String): MutableList<JvLineItemResponse>
}
