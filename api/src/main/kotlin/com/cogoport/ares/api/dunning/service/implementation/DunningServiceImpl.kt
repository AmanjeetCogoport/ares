package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.AresConstants.CREDIT_DAYS_MAPPING
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.dunning.entity.CreditController
import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.model.DunningExceptionType
import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.repository.CreditControllerRepo
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AuditRepository
import com.cogoport.ares.api.utils.ExcelUtils
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.AuditActionName
import com.cogoport.ares.model.common.AuditObjectType
import com.cogoport.ares.model.common.GetOrganizationTradePartyDetailRequest
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.enum.DunningCatagory
import com.cogoport.ares.model.dunning.enum.DunningExecutionFrequency
import com.cogoport.ares.model.dunning.enum.ScheduleType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilterRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.excel.utils.ExcelSheetReader
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class DunningServiceImpl(
    private var creditControllerRepo: CreditControllerRepo,
    private var accountUtilizationRepo: AccountUtilizationRepo,
    private var dunningCycleRepo: DunningCycleRepo,
    private var dunningExecutionRepo: DunningCycleExecutionRepo,
    private var auditRepository: AuditRepository,
    private val masterExceptionRepo: MasterExceptionRepo,
    private val cogoBackLowLevelClient: CogoBackLowLevelClient,
    private val cycleExceptionRepo: CycleExceptionRepo,
    private val util: Util,
    private val authClient: AuthClient
) : DunningService {

    @Transactional
    override suspend fun createCreditController(creditControllerRequest: CreditControllerRequest): Long {
        if (creditControllerRequest.createdBy == null) throw AresException(
            AresError.ERR_1003,
            " : created by can't be null"
        )
        val response = creditControllerRepo.save(
            CreditController(
                id = null,
                creditControllerName = creditControllerRequest.creditControllerName,
                creditControllerId = creditControllerRequest.creditControllerId,
                organizationId = creditControllerRequest.organizationId,
                organizationSegment = creditControllerRequest.organizationSegment,
                createdAt = null,
                updatedAt = null,
                createdBy = creditControllerRequest.createdBy,
                updatedBy = creditControllerRequest.updatedBy
            )
        )

        return response.id!!
    }

    @Transactional
    override suspend fun updateCreditController(updateCreditController: UpdateCreditControllerRequest): Long {
        val creditController = creditControllerRepo.findById(Hashids.decode(updateCreditController.id)[0])
            ?: throw AresException(AresError.ERR_1541, "")

        val updateCreditController: CreditController = CreditController(
            id = Hashids.decode(updateCreditController.id)[0],
            creditControllerName = updateCreditController.creditControllerName
                ?: creditController.creditControllerName,
            creditControllerId = updateCreditController.creditControllerId
                ?: creditController.creditControllerId,
            organizationId = creditController?.organizationId,
            organizationSegment = updateCreditController.organizationSegment
                ?: creditController.organizationSegment,
            createdBy = creditController?.createdBy,
            updatedBy = updateCreditController.updatedBy,
            createdAt = creditController?.createdAt,
            updatedAt = creditController?.updatedAt
        )

        val response = creditControllerRepo.update(updateCreditController)

        return response.id!!
    }

    override suspend fun createDunningCycle(createDunningCycleRequest: CreateDunningCycleRequest): Long {
        if (createDunningCycleRequest.name.length < 3) throw AresException(AresError.ERR_1544, "")

        if (createDunningCycleRequest.triggerType == TriggerType.ONE_TIME && createDunningCycleRequest.scheduleType != ScheduleType.ONE_TIME) {
            throw AresException(AresError.ERR_1003, "")
        } else if (createDunningCycleRequest.triggerType == TriggerType.PERIODIC && createDunningCycleRequest.scheduleType == ScheduleType.ONE_TIME) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (createDunningCycleRequest.triggerType == TriggerType.PERIODIC &&
            createDunningCycleRequest.scheduleType == ScheduleType.MONTHLY &&
            createDunningCycleRequest.scheduleRule.dayOfMonth == null &&
            createDunningCycleRequest.scheduleRule.dunningExecutionFrequency != DunningExecutionFrequency.MONTHLY
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (createDunningCycleRequest.triggerType == TriggerType.PERIODIC &&
            createDunningCycleRequest.scheduleType == ScheduleType.WEEKLY &&
            createDunningCycleRequest.scheduleRule.week == null &&
            createDunningCycleRequest.scheduleRule.dunningExecutionFrequency != DunningExecutionFrequency.WEEKLY
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        val dunningCycleResponse = dunningCycleRepo.save(
            DunningCycle(
                id = null,
                name = createDunningCycleRequest.name,
                cycleType = createDunningCycleRequest.cycleType,
                triggerType = createDunningCycleRequest.triggerType,
                scheduleType = createDunningCycleRequest.scheduleType,
                severityLevel = createDunningCycleRequest.severityLevel,
                filters = createDunningCycleRequest.filters,
                scheduleRule = createDunningCycleRequest.scheduleRule,
                templateId = createDunningCycleRequest.templateId,
                category = createDunningCycleRequest.category ?: DunningCatagory.CYCLE,
                isActive = createDunningCycleRequest.isActive ?: true,
                deletedAt = null,
                createdBy = createDunningCycleRequest.createdBy,
                updatedBy = createDunningCycleRequest.createdBy,
                createdAt = null,
                updatedAt = null
            )
        )

        val organizationTradePartyDetailResponse = authClient.getOrganizationTradePartyDetail(
            GetOrganizationTradePartyDetailRequest(
                organizationTradePartyDetailIds = createDunningCycleRequest.exceptionTradePartyDetailIds
            )
        )

        val dunningCycleExceptionList: MutableList<CycleExceptions> = mutableListOf()
        organizationTradePartyDetailResponse.forEach { organizationTradePartyDetail ->
            dunningCycleExceptionList.add(
                CycleExceptions(
                    id = null,
                    cycleId = dunningCycleResponse.id!!,
                    tradePartyDetailId = organizationTradePartyDetail.organizationTradePartyDetailId,
                    registrationNumber = organizationTradePartyDetail.registrationNumber,
                    deletedAt = null,
                    createdBy = dunningCycleResponse.createdBy,
                    updatedBy = dunningCycleResponse.updatedBy,
                    createdAt = null,
                    updatedAt = null
                )
            )
        }

        cycleExceptionRepo.saveAll(dunningCycleExceptionList)

        TODO("Write trigger for rabbitMQ.")

        // TODO("WRITE ALGO TO CALCULATE NEXT DUNNING CYCLE.")
        val dunningCycleScheduledAt: Timestamp = Timestamp(System.currentTimeMillis())

        val dunningCycleExecutionResponse = dunningExecutionRepo.save(
            DunningCycleExecution(
                id = null,
                dunningCycleId = dunningCycleResponse.id!!,
                templateId = dunningCycleResponse.templateId!!,
                status = CycleExecutionStatus.SCHEDULED,
                filters = dunningCycleResponse.filters,
                scheduleRule = dunningCycleResponse.scheduleRule,
                scheduleType = dunningCycleResponse.scheduleType,
                scheduleAt = dunningCycleScheduledAt,
                triggerType = dunningCycleResponse.triggerType,
                deletedAt = dunningCycleResponse.deletedAt,
                createdBy = dunningCycleResponse.createdBy,
                updatedBy = dunningCycleResponse.updatedBy,
                createdAt = null,
                updatedAt = null
            )
        )

        auditRepository.save(
            Audit(
                id = null,
                objectType = AuditObjectType.DUNNING_CYCLE.value,
                objectId = dunningCycleResponse.id,
                actionName = AuditActionName.CREATE.value,
                data = dunningCycleResponse,
                performedBy = dunningCycleResponse.createdBy,
                performedByUserType = null,
                createdAt = null
            )
        )

        auditRepository.save(
            Audit(
                id = null,
                objectType = AuditObjectType.DUNNING_CYCLE_EXECUTION.value,
                objectId = dunningCycleExecutionResponse.id,
                actionName = AuditActionName.CREATE.value,
                data = dunningCycleExecutionResponse,
                performedBy = dunningCycleExecutionResponse.createdBy,
                performedByUserType = null,
                createdAt = null
            )
        )
    }

    override suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): List<CustomerOutstandingAndOnAccountResponse> {
        var serviceTypes = listOf<ServiceType>()

        request.serviceTypes?.forEach { serviceType ->
            serviceTypes = serviceTypes + getServiceType(serviceType)
        }

        var taggedOrganizationIds: List<UUID>? = null
        if (request.creditControllerIds != null) {
            taggedOrganizationIds = creditControllerRepo.listOrganizationIdBasedOnCreditControllers(
                creditControllerIds = request.creditControllerIds!!
            )
        }

        val response = listOnAccountAndOutstandingBasedOnDunninCycleFilters(
            DunningCycleFilterRequest(
                entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS[request.cogoEntityId.toString()]!!,
                serviceTypes = serviceTypes,
                taggedOrganizationIds = taggedOrganizationIds,
                totalDueOutstanding = request.totalDueOutstanding,
                ageingStartDay = getAgeingBucketDays(request.ageingBucket)[0],
                ageingLastDay = getAgeingBucketDays(request.ageingBucket)[1],
                exceptionTradePartyDetailId = null
            )
        )

        return response
    }

    open suspend fun listOnAccountAndOutstandingBasedOnDunninCycleFilters(
        dunningCycleFilterRequest: DunningCycleFilterRequest
    ): List<CustomerOutstandingAndOnAccountResponse> {
        val customerOutstandingAndOnAccountResponses: List<CustomerOutstandingAndOnAccountResponse> =
            accountUtilizationRepo.listOnAccountAndOutstandingsBasedOnDunninCycleFilters(
                entityCode = dunningCycleFilterRequest.entityCode,
                serviceTypes = dunningCycleFilterRequest.serviceTypes,
                ageingStartDay = dunningCycleFilterRequest.ageingStartDay,
                ageingLastDay = dunningCycleFilterRequest.ageingLastDay,
                onAccountAccountType = AresConstants.ON_ACCOUNT_ACCOUNT_TYPE,
                outstandingAccountType = AresConstants.OUTSTANDING_ACCOUNT_TYPE,
                taggedOrganizationIds = dunningCycleFilterRequest.taggedOrganizationIds
            )

        val response = customerOutstandingAndOnAccountResponses.filter { customerOutstandingAndOnAccountResponse ->
            customerOutstandingAndOnAccountResponse.outstandingAmount > dunningCycleFilterRequest.totalDueOutstanding
        }

        response.forEach { r ->
            r.outstandingAmount = r.outstandingAmount + r.onAccountAmount
        }

        var responseList = listOf<CustomerOutstandingAndOnAccountResponse>()

        if (dunningCycleFilterRequest.exceptionTradePartyDetailId != null) {
            val customerToRemove = dunningCycleFilterRequest.exceptionTradePartyDetailId!!.map { it }
            responseList = response.filter { customer -> customer.tradePartyDetailId !in customerToRemove }
        } else {
            responseList = response
        }

        return responseList
    }

    private fun getAgeingBucketDays(ageingBucketName: AgeingBucketEnum): IntArray {
        return when (ageingBucketName) {
            AgeingBucketEnum.ALL -> intArrayOf(0, 0)
            AgeingBucketEnum.AB_0_30 -> intArrayOf(0, 30)
            AgeingBucketEnum.AB_31_60 -> intArrayOf(31, 60)
            AgeingBucketEnum.AB_61_90 -> intArrayOf(61, 90)
            AgeingBucketEnum.AB_91_180 -> intArrayOf(91, 180)
            AgeingBucketEnum.AB_181_PLUS -> intArrayOf(180, 180)
            else -> throw AresException(AresError.ERR_1542, "")
        }
    }

    private fun getServiceType(serviceType: ServiceType): List<ServiceType> {
        return when (serviceType) {
            ServiceType.FCL_FREIGHT -> listOf<ServiceType>(
                ServiceType.FCL_FREIGHT, ServiceType.FCL_CUSTOMS, ServiceType.FCL_FREIGHT_LOCAL,
                ServiceType.FCL_CUSTOMS, ServiceType.FCL_CFS
            )

            ServiceType.LCL_FREIGHT -> listOf<ServiceType>(
                ServiceType.LCL_FREIGHT, ServiceType.LCL_CUSTOMS, ServiceType.LCL_CUSTOMS_FREIGHT
            )

            ServiceType.LTL_FREIGHT -> listOf<ServiceType>(
                ServiceType.LTL_FREIGHT
            )

            ServiceType.AIR_FREIGHT -> listOf<ServiceType>(
                ServiceType.AIR_FREIGHT, ServiceType.AIR_CUSTOMS_FREIGHT, ServiceType.AIR_CUSTOMS,
                ServiceType.AIR_FREIGHT_LOCAL, ServiceType.DOMESTIC_AIR_FREIGHT
            )

            ServiceType.FTL_FREIGHT -> listOf<ServiceType>(
                ServiceType.FTL_FREIGHT
            )

            ServiceType.HAULAGE_FREIGHT -> listOf<ServiceType>(
                ServiceType.HAULAGE_FREIGHT
            )

            else -> throw AresException(AresError.ERR_1543, "")
        }
    }

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

//    override suspend fun listDunningCycles(request: ListDunningCycleReq): ResponseList<ListDunningCycleResp> {
//
//    }

    override suspend fun deleteOrUpdateMasterException(id: String, updatedBy: UUID, actionType: String): Boolean {
        val exceptionId = Hashids.decode(id)[0]
        masterExceptionRepo.deleteOrUpdateException(exceptionId, updatedBy, actionType)
        return true
    }

    override suspend fun listDunningCycleExecution(request: ListDunningCycleExecutionReq): List<DunningCycleExecution> {
        val response = dunningExecutionRepo.listDunningCycleExecution()
    }

    override suspend fun createDunningException(request: CreateDunningException): MutableList<String> {
        if (request.exceptionType == DunningExceptionType.CYCLE_WISE && request.cycleId.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, "cycle id")
        }
        if (request.exceptionType == DunningExceptionType.CYCLE_WISE && request.actionType.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, "action type")
        }
        if (request.exceptionFile.isNullOrEmpty() && request.excludedRegistrationNos.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, "file and exclusion list both can not be empty")
        }
        val finalExcludedPans: MutableList<String> = if (!request.exceptionFile.isNullOrEmpty()) {
            handleExceptionFile(request)
        } else {
            request.excludedRegistrationNos!!
        }
        if (request.exceptionType == DunningExceptionType.CYCLE_WISE) {
            return saveCycleWiseExceptions(request, finalExcludedPans)
        }
        return saveMasterExceptions(request, finalExcludedPans)
    }

    private suspend fun saveCycleWiseExceptions(
        request: CreateDunningException,
        regNos: MutableList<String>
    ): MutableList<String> {
        val response = cogoBackLowLevelClient.getTradePartyDetailsByRegistrationNumber(
            regNos,
            "list_organization_trade_party_details"
        )
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
        if (request.actionType == "DELETE") {
            val result = cycleExceptionRepo.deleteExceptionByCycleId(cycleId, finalDetailIds)
            return mutableListOf(Hashids.encode(result))
        }
        val savedResponse = cycleExceptionRepo.saveAll(exceptionEntity)
        val returnResponse = mutableListOf<String>()
        savedResponse.forEach { returnResponse.add(Hashids.encode(it.id!!)) }
        return returnResponse
    }

    private suspend fun saveMasterExceptions(
        request: CreateDunningException,
        regNos: MutableList<String>
    ): MutableList<String> {
        val response =
            cogoBackLowLevelClient.getTradePartyDetailsByRegistrationNumber(regNos, "list_organization_trade_parties")
        val filteredList = response?.list?.distinctBy { it["organization_trade_party_detail_id"] }
        val masterExceptionList = masterExceptionRepo.getAllMasterExceptions()
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
