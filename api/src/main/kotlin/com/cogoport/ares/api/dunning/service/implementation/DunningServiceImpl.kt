package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants.CREDIT_DAYS_MAPPING
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.model.DunningExceptionType
import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.utils.ExcelUtils
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.brahma.excel.utils.ExcelSheetReader
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class DunningServiceImpl(
    private val masterExceptionRepo: MasterExceptionRepo,
    private val cogoBackLowLevelClient: CogoBackLowLevelClient,
    private val cycleExceptionRepo: CycleExceptionRepo,
    private val util: Util
) : DunningService {
    override suspend fun listMasterException(request: ListExceptionReq): ResponseList<MasterExceptionResp> {
        val query = util.toQueryString(request.query)
        var creditDaysFrom: Long? = null
        var creditDaysTo: Long? = null
        if (request.creditDays != null) {
            creditDaysFrom = CREDIT_DAYS_MAPPING[request.creditDays]?.first
            creditDaysTo = CREDIT_DAYS_MAPPING[request.creditDays]?.second
        }
        var q: String? = null
        if (request.query != null) {
            q = util.toQueryString(request.query)
        }
        val responseList = masterExceptionRepo.listMasterException(
            q,
            request.segmentation,
            request.pageIndex,
            request.pageSize,
            creditDaysFrom,
            creditDaysTo,
            request.sortBy ?: "DESC",
            request.sortType ?: "dueAmount"
        )
        val totalCount = masterExceptionRepo.listMasterExceptionTotalCount(
            q,
            request.segmentation,
        )
        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize)
        return ResponseList(
            list = responseList,
            totalPages = totalPages,
            totalRecords = totalCount,
            pageNo = request.pageIndex
        )
    }

    override suspend fun getCycleWiseExceptions(request: ListExceptionReq): ResponseList<CycleWiseExceptionResp> {
        if (request.cycleId == null) throw AresException(AresError.ERR_1003, "cycle Id")
        var cycleId = Hashids.decode(request.cycleId!!)[0]
        var q: String? = null
        if (request.query != null) {
            q = util.toQueryString(request.query)
        }
        val listResponse = cycleExceptionRepo.listExceptionByCycleId(
            q,
            cycleId,
            request.pageSize,
            request.pageIndex
        )
        val totalCount = cycleExceptionRepo.getListExceptionByCycleIdTotalCount(
            q,
            cycleId,
        )
        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize)
        return ResponseList(
            list = listResponse,
            totalPages = totalPages,
            totalRecords = totalCount,
            pageNo = request.pageIndex
        )
    }

    override suspend fun createDunningException(request: CreateDunningException): MutableList<String> {
        if (request.exceptionType == DunningExceptionType.CYCLE_WISE && request.cycleId.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1541, "")
        }
        var finalExcludedPans: MutableList<String> = mutableListOf()
        if (!request.exceptionFile.isNullOrEmpty()) {
            finalExcludedPans = handleExceptionFile(request)
        }
        if (request.exceptionType == DunningExceptionType.CYCLE_WISE) {
            return saveCycleWiseExceptions(request, finalExcludedPans)
        }
        return saveMasterExceptions(request, finalExcludedPans)
    }
    private suspend fun saveCycleWiseExceptions(request: CreateDunningException, regNos: MutableList<String>): MutableList<String> {
        val response = cogoBackLowLevelClient.getTradePartyDetailsByRegistrationNumber(regNos, "list_organization_trade_party_details")
        val cycleId = Hashids.decode(request.cycleId!!)[0]
        val finalDetailIds = mutableListOf<UUID>()
        val exceptionEntity = mutableListOf<CycleExceptions>()
        val activeExceptions = cycleExceptionRepo.getActiveExceptionsByCycle(cycleId)
        response?.list?.forEach { t ->
            val tradePartyDetailId = UUID.fromString(t["id"].toString())
            finalDetailIds.add(tradePartyDetailId)
            if (activeExceptions?.firstOrNull { it.tradePartyDetailId == tradePartyDetailId } == null) {
                exceptionEntity.add(
                    CycleExceptions(
                        id = null,
                        cycleId = cycleId,
                        tradePartyDetailId = tradePartyDetailId,
                        registrationNumber = t["registration_number"].toString(),
                        createdBy = request.createdBy,
                        updatedBy = request.createdBy
                    )
                )
            }
        }
        cycleExceptionRepo.deleteExceptionByCycleId(cycleId, finalDetailIds)
        val savedResponse = cycleExceptionRepo.saveAll(exceptionEntity)
        val returnResponse = mutableListOf<String>()
        savedResponse.forEach { returnResponse.add(Hashids.encode(it.id!!)) }
        return returnResponse
    }
    private suspend fun saveMasterExceptions(request: CreateDunningException, regNos: MutableList<String>): MutableList<String> {
        val response = cogoBackLowLevelClient.getTradePartyDetailsByRegistrationNumber(regNos, "list_organization_trade_parties")
        val filteredList = response?.list?.distinctBy { it["organization_trade_party_detail_id"] }
        val masterExceptionList = masterExceptionRepo.getActiveMasterExceptions()
        val exceptionEntity = mutableListOf<MasterExceptions>()
        filteredList?.forEach { t ->
            val tradePartyDetailId = UUID.fromString(t["organization_trade_party_detail_id"].toString())
            if (masterExceptionList?.firstOrNull { it.tradePartyDetailId == tradePartyDetailId } == null) {
                exceptionEntity.add(
                    MasterExceptions(
                        id = null,
                        tradePartyDetailId = tradePartyDetailId,
                        tradePartyName = t["legal_business_name"].toString(),
                        organizationId = UUID.fromString(t["organization_id"].toString()),
                        registrationNumber = t["registration_number"].toString(),
                        orgSegment = null,
                        creditDays = 0,
                        creditAmount = 0,
                        isActive = true,
                        createdBy = request.createdBy,
                        updatedBy = request.createdBy
                    )
                )
            }
        }
        val savedResponse = masterExceptionRepo.saveAll(exceptionEntity)
        val returnResponse = mutableListOf<String>()
        savedResponse.forEach { returnResponse.add(Hashids.encode(it.id!!)) }
        return returnResponse
    }

    private fun handleExceptionFile(request: CreateDunningException): MutableList<String> {
        val file = ExcelUtils.downloadExcelFile(request.exceptionFile!!)
        val excelSheetReader = ExcelSheetReader(file)
        val exclusionData = excelSheetReader.read()
        val noOfColumns = exclusionData.first().size
        file.delete()
        if (noOfColumns != 2) {
            throw AresException(AresError.ERR_1511, "Number of columns mismatch")
        }
        if (exclusionData.isEmpty()) {
            throw AresException(AresError.ERR_1511, "No Data found!")
        }
        var returnExclusionList = request.excludedRegistrationNos
        if (returnExclusionList.isNullOrEmpty()) {
            returnExclusionList = mutableListOf()
        }
        exclusionData.forEach {
            returnExclusionList.add(it["registration number"].toString())
        }
        return returnExclusionList.toSet().toMutableList()
    }
}
