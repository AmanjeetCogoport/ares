package com.cogoport.ares.api.balances.service.implementation

import com.cogoport.ares.api.balances.entity.LedgerBalance
import com.cogoport.ares.api.balances.repository.LedgerBalanceRepo
import com.cogoport.ares.api.balances.service.interfaces.LedgerBalanceService
import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.balances.request.ListLedgerBalancesReq
import com.cogoport.ares.model.common.ResponseList
import jakarta.inject.Singleton
import java.util.Date
import java.util.UUID

@Singleton
class LedgerBalanceServiceImpl(
    private val ledgerBalanceRepo: LedgerBalanceRepo,
    private var accountUtilizationRepository: AccountUtilizationRepository,
) : LedgerBalanceService {

    override suspend fun getLedgerBalances(listOpeningBalanceReq: ListLedgerBalancesReq): ResponseList<LedgerBalance> {
        var query: String? = null
        if (!listOpeningBalanceReq.q.isNullOrEmpty()) {
            query = "%${listOpeningBalanceReq.q}%"
        }
        val openingBalList = ledgerBalanceRepo.listLedgerBalances(
            query,
            listOpeningBalanceReq.entityCode!!,
            listOpeningBalanceReq.date!!,
            listOpeningBalanceReq.pageIndex,
            listOpeningBalanceReq.pageSize,
            listOpeningBalanceReq.sortField,
            listOpeningBalanceReq.sortType
        )

        val totalCount = ledgerBalanceRepo.countLedgerBalances(
            query,
            listOpeningBalanceReq.entityCode!!,
            listOpeningBalanceReq.date!!
        )
        val totalPages = Utilities.getTotalPages(totalCount, listOpeningBalanceReq.pageSize)
        val responseList = ResponseList<LedgerBalance>()
        responseList.list = openingBalList
        responseList.totalPages = totalPages
        responseList.totalRecords = totalCount
        responseList.pageNo = listOpeningBalanceReq.pageIndex
        return responseList
    }

    suspend fun createLedgerBalances(currentDate: Date, entityCode: Int) {
        val openingBalances = accountUtilizationRepository.getLedgerBalances(currentDate, entityCode)
            ?: return
        val ledgerBalanceList = mutableListOf<LedgerBalance>()
        openingBalances.forEach {
            ledgerBalanceList.add(
                LedgerBalance(
                    null,
                    tradePartyDetailId = it.tradePartyDetailId,
                    balanceAmount = it.balanceAmount,
                    balanceDate = currentDate,
                    ledgerCurrency = it.ledgerCurrency,
                    entityId = UUID.fromString(AresConstants.ENTITY_ID[entityCode]),
                    entityCode = entityCode
                )
            )
        }
        ledgerBalanceRepo.saveAll(ledgerBalanceList)
    }
}
