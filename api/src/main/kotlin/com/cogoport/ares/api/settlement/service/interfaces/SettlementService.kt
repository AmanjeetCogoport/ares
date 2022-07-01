package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.SettlementInvoiceRequest

interface SettlementService {

    suspend fun getInvoices(request: SettlementInvoiceRequest): ResponseList<AccountUtilizationResponse>?
}
