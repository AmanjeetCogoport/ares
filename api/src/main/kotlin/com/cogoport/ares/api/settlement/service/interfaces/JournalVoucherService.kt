package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JvLineItemResponse
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import com.cogoport.ares.model.settlement.request.JVBulkFileUploadRequest
import java.math.BigDecimal
import java.util.UUID

interface JournalVoucherService {

    suspend fun updateJournalVoucherStatus(id: Long, isUtilized: Boolean, performedBy: UUID, performedByUserType: String?, documentValue: String?)

    suspend fun createJvAccUtil(request: JournalVoucher, accMode: AccMode, signFlag: Short, settlementEnabled: Boolean): AccountUtilization

    suspend fun getJVLineItems(parentJVId: String): MutableList<JvLineItemResponse>

    suspend fun createTdsJvLineItems(
        parentJvData: ParentJournalVoucher,
        accountUtilization: AccountUtilization?,
        jvLineItems: MutableList<HashMap<String, Any?>>,
        tdsAmount: BigDecimal?,
        tdsLedAmount: BigDecimal?,
        createdByUserType: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?,
        utr: String?
    ): Long?

    fun makeJournalVoucherLineItem(
        parentMapping: HashMap<String, ParentJournalVoucher>,
        journalVouchers: List<Map<String, Any>>,
        jvBulkFileUploadRequest: JVBulkFileUploadRequest,
        tradePartyDetails: Map<String, ListOrganizationTradePartyDetailsResponse>,
        documentId: Long?
    ): List<JournalVoucher>
}
