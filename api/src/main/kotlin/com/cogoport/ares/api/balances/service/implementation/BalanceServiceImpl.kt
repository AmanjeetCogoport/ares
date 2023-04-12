package com.cogoport.ares.api.balances.service.implementation

import com.cogoport.ares.api.balances.entity.OpeningBalance
import com.cogoport.ares.api.balances.repository.OpeningBalanceRepo
import com.cogoport.ares.api.balances.service.interfaces.BalanceService
import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.balances.request.ListOpeningBalancesReq
import com.cogoport.ares.model.common.ResponseList
import jakarta.inject.Singleton
import java.util.Date
import java.util.UUID

@Singleton
class BalanceServiceImpl(
    private val openingBalanceRepo: OpeningBalanceRepo,
    private var accountUtilizationRepository: AccountUtilizationRepository,
) : BalanceService {

    override suspend fun getOpeningBalances(listOpeningBalanceReq: ListOpeningBalancesReq): ResponseList<OpeningBalance> {
        var query: String? = null
        if (!listOpeningBalanceReq.q.isNullOrEmpty()) {
            query = "%${listOpeningBalanceReq.q}%"
        }
        val openingBalList = openingBalanceRepo.listOpeningBalances(
            query,
            listOpeningBalanceReq.entityCode!!,
            listOpeningBalanceReq.date!!,
            listOpeningBalanceReq.pageIndex,
            listOpeningBalanceReq.pageSize,
            listOpeningBalanceReq.sortField,
            listOpeningBalanceReq.sortType
        ) ?: throw AresException(AresError.ERR_1002, " No opening balances found")

        val totalCount = openingBalanceRepo.countOpeningBalances(
            query,
            listOpeningBalanceReq.entityCode!!,
            listOpeningBalanceReq.date!!
        )
        val totalPages = Utilities.getTotalPages(totalCount, listOpeningBalanceReq.pageSize)
        val responseList = ResponseList<OpeningBalance>()
        responseList.list = openingBalList
        responseList.totalPages = totalPages
        responseList.totalRecords = totalCount
        responseList.pageNo = listOpeningBalanceReq.pageIndex
        return responseList
    }

    suspend fun createOpeningBalances(currentDate: Date, entityCode: Int) {
        val openingBalances = accountUtilizationRepository.getOpeningBalances(currentDate, entityCode)
            ?: throw AresException(AresError.ERR_1002, " No Opening balances found for $entityCode for $currentDate")
        val openingBalanceList = mutableListOf<OpeningBalance>()
        openingBalances.forEach {
            openingBalanceList.add(
                OpeningBalance(
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
        openingBalanceRepo.saveAll(openingBalanceList)
    }
}
