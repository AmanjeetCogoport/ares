package com.cogoport.ares.api.balances.service.interfaces

import com.cogoport.ares.api.balances.entity.LedgerBalance
import com.cogoport.ares.model.balances.request.ListLedgerBalancesReq
import com.cogoport.ares.model.common.ResponseList

interface LedgerBalanceService {

    suspend fun getLedgerBalances(listOpeningBalanceReq: ListLedgerBalancesReq): ResponseList<LedgerBalance>
}
