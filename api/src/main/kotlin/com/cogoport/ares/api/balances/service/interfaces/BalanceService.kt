package com.cogoport.ares.api.balances.service.interfaces

import com.cogoport.ares.api.balances.entity.OpeningBalance
import com.cogoport.ares.model.balances.request.ListOpeningBalancesReq
import com.cogoport.ares.model.common.ResponseList

interface BalanceService {

    suspend fun getOpeningBalances(listOpeningBalanceReq: ListOpeningBalancesReq): ResponseList<OpeningBalance>
}