package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.payment.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.InvoiceListResponse

interface SettlementService {

    suspend fun getInvoices(request: SettlementInvoiceRequest): ResponseList<InvoiceListResponse>?
}
