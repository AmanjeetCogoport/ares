package com.cogoport.ares.api.balances.controller

import com.cogoport.ares.api.balances.entity.LedgerBalance
import com.cogoport.ares.api.balances.service.interfaces.LedgerBalanceService
import com.cogoport.ares.model.balances.request.ListLedgerBalancesReq
import com.cogoport.ares.model.common.ResponseList
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import javax.validation.Valid

@Validated
@Controller("/balances")
class LedgerBalanceController(
    private val ledgerBalanceService: LedgerBalanceService
) {

    @Get("/list{?requests*}")
    suspend fun listLedgerBalances(@Valid requests: ListLedgerBalancesReq): ResponseList<LedgerBalance> {
        return ledgerBalanceService.getLedgerBalances(requests)
    }
}
