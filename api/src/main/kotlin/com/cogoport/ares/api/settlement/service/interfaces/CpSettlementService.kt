package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse

interface CpSettlementService {

    suspend fun getInvoices(settlementInvoiceRequest: SettlementInvoiceRequest): ResponseList<SettlementInvoiceResponse>
    suspend fun knockoff(request: SettlementKnockoffRequest): SettlementKnockoffResponse
}
