package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.payment.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.SettlementInvoiceRequest

class SettlementServiceImpl : SettlementService {
    override suspend fun getInvoices(request: SettlementInvoiceRequest): ResponseList<AccountUtilizationResponse>? {
        TODO("Not yet implemented")
    }
}
