package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.payment.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.InvoiceListResponse
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse

interface SettlementService {

    suspend fun getInvoices(request: SettlementInvoiceRequest): ResponseList<InvoiceListResponse>?

    suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse

    suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse
}
