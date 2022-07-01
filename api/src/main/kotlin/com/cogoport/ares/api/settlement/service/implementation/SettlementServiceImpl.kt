package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.SettlementInvoiceRequest

class SettlementServiceImpl : SettlementService {
    override suspend fun getInvoices(request: SettlementInvoiceRequest): ResponseList<AccountUtilizationResponse>? {
        TODO("Not yet implemented")
    }

    private fun getInvoicesFromOpenSearch(request: AccountCollectionRequest){
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val response = mutableListOf<AccountUtilizationResponse?>()
        val data = OpenSearchClient().onAccountSearch(request, AccountUtilizationResponse::class.java)!!
    }
}
