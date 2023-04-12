package com.cogoport.ares.api.balances.controller

import com.cogoport.ares.api.balances.entity.OpeningBalance
import com.cogoport.ares.api.balances.service.interfaces.BalanceService
import com.cogoport.ares.model.balances.request.ListOpeningBalancesReq
import com.cogoport.ares.model.common.ResponseList
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import javax.validation.Valid

@Validated
@Controller("/balances")
class BalanceController(
    private val balanceService: BalanceService
) {

    @Get("/list{?requests*}")
    suspend fun listOpeningBalances(@Valid requests: ListOpeningBalancesReq): ResponseList<OpeningBalance> {
        return balanceService.getOpeningBalances(requests)
    }
}
