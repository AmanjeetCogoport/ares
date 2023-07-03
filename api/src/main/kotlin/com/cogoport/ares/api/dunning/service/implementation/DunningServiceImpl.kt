package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.AresConstants.CREDIT_DAYS_MAPPING
import com.cogoport.ares.api.common.AresConstants.SEGMENT_MAPPING
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.entity.OrganizationStakeholder
import com.cogoport.ares.api.dunning.model.DunningExceptionType
import com.cogoport.ares.api.dunning.model.SeverityEnum
import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.ListDunningCycleReq
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.repository.OrganizationStakeholderRepo
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
import com.cogoport.ares.model.common.GetOrganizationTradePartyDetailResponse
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.enum.DunningCategory
import com.cogoport.ares.model.dunning.enum.DunningCycleStatus
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.dunning.enum.DunningExecutionFrequency
import com.cogoport.ares.model.dunning.enum.FREQUENCY
import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import com.cogoport.ares.model.dunning.enum.OrganizationStakeholderType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilterRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.ListOrganizationStakeholderRequest
import com.cogoport.ares.model.dunning.request.MonthWiseStatisticsOfAccountUtilizationReuest
import com.cogoport.ares.model.dunning.request.OverallOutstandingAndOnAccountRequest
import com.cogoport.ares.model.dunning.request.SyncOrgStakeholderRequest
import com.cogoport.ares.model.dunning.request.UpdateCycleExecutionRequest
import com.cogoport.ares.model.dunning.request.UpdateDunningCycleExecutionStatusReq
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.ares.model.dunning.response.DunningCycleResponse
import com.cogoport.ares.model.dunning.response.MonthWiseStatisticsOfAccountUtilizationResponse
import com.cogoport.ares.model.dunning.response.OverallOutstandingAndOnAccountResponse
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.excel.utils.ExcelSheetReader
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.rabbitmq.client.interfaces.RabbitmqService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class DunningServiceImpl(
    private var organizationStakeholderRepo: OrganizationStakeholderRepo,
    private var accountUtilizationRepo: AccountUtilizationRepo,
    private var dunningCycleRepo: DunningCycleRepo,
    private var dunningExecutionRepo: DunningCycleExecutionRepo,
    private var auditRepository: AuditRepository,
    private val masterExceptionRepo: MasterExceptionRepo,
    private val cogoBackLowLevelClient: CogoBackLowLevelClient,
    private val cycleExceptionRepo: CycleExceptionRepo,
    private val util: Util,
    private val authClient: AuthClient,
    private val rabbitMq: RabbitmqService
) : DunningService {

    @Transactional
    override suspend fun syncOrgStakeholders(syncOrgStakeholderRequest: SyncOrgStakeholderRequest): Long {
        val organizationStakeholderType = OrganizationStakeholderType.valueOf(
            syncOrgStakeholderRequest.organizationStakeholderType!!
                .replace("_", " ").toUpperCase().replace(" ", "_")
        ).toString()

        var organizationStakeholder = organizationStakeholderRepo.getOrganizationStakeholdersUsingOrgId(
            organizationId = syncOrgStakeholderRequest.organizationId!!,
            organizationStakeholderType = organizationStakeholderType
        )

        if (organizationStakeholder == null) {

            val organizationSegment = syncOrgStakeholderRequest.organizationSegment!!
                .replace("_", " ").toUpperCase().replace(" ", "_")

            val organizationStakeholderEntity = OrganizationStakeholder(
                id = null,
                organizationStakeholderName = syncOrgStakeholderRequest.organizationStakeholderName!!,
                organizationStakeholderType = organizationStakeholderType,
                organizationId = syncOrgStakeholderRequest.organizationId!!,
                organizationSegment = OrganizationSegment.valueOf(organizationSegment).toString(),
                organizationStakeholderId = syncOrgStakeholderRequest.organizationId!!,
                createdBy = UUID.fromString(AresConstants.ARES_USER_ID),
                updatedBy = UUID.fromString(AresConstants.ARES_USER_ID),
                createdAt = null,
                updatedAt = null
            )
            organizationStakeholder = organizationStakeholderRepo.save(organizationStakeholderEntity)
        } else {
            var organizationSegment = organizationStakeholder.organizationSegment
            if (syncOrgStakeholderRequest.organizationSegment != null) {
                organizationSegment = OrganizationSegment.valueOf(
                    syncOrgStakeholderRequest.organizationSegment!!
                        .replace("_", " ").toUpperCase().replace(" ", "_")
                ).toString()
            }

            var isActive = organizationStakeholder.isActive
            if (syncOrgStakeholderRequest.status != null) {
                isActive = syncOrgStakeholderRequest.status == "Active"
            }

            organizationStakeholder.organizationId = syncOrgStakeholderRequest.organizationId
                ?: organizationStakeholder.organizationId
            organizationStakeholder.organizationStakeholderId = syncOrgStakeholderRequest.organizationStakeholderId
                ?: organizationStakeholder.organizationStakeholderId
            organizationStakeholder.organizationStakeholderType = organizationStakeholderType
            organizationStakeholder.organizationSegment = organizationSegment
            organizationStakeholder.organizationStakeholderName = syncOrgStakeholderRequest.organizationStakeholderName
                ?: organizationStakeholder.organizationStakeholderName
            organizationStakeholder.isActive = isActive
            organizationStakeholder.updatedBy = syncOrgStakeholderRequest.updatedBy ?: organizationStakeholder.updatedBy

            organizationStakeholderRepo.update(organizationStakeholder)
        }

        return organizationStakeholder.id!!
    }

    @Transactional
    override suspend fun createDunningCycle(createDunningCycleRequest: CreateDunningCycleRequest): Long {
        if (createDunningCycleRequest.name.length < 5) throw AresException(AresError.ERR_1544, "")
        if (createDunningCycleRequest.scheduleRule.scheduleTime.length != 5) throw AresException(AresError.ERR_1547, "")

        if (
            TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.ONE_TIME &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) != FREQUENCY.ONE_TIME
        ) {
            throw AresException(AresError.ERR_1003, "")
        } else if (
            TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.ONE_TIME
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (
            TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.MONTHLY &&
            createDunningCycleRequest.scheduleRule.dunningExecutionFrequency?.let { DunningExecutionFrequency.valueOf(it) }
            != DunningExecutionFrequency.MONTHLY &&
            createDunningCycleRequest.scheduleRule.dayOfMonth == null &&
            createDunningCycleRequest.scheduleRule.dayOfMonth!! < 1 &&
            createDunningCycleRequest.scheduleRule.dayOfMonth!! > AresConstants.MAX_DAY_IN_MONTH_FOR_DUNNING
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (
            TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.WEEKLY &&
            createDunningCycleRequest.scheduleRule.dunningExecutionFrequency?.let { DunningExecutionFrequency.valueOf(it) }
            != DunningExecutionFrequency.WEEKLY &&
            createDunningCycleRequest.scheduleRule.week == null
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.ONE_TIME) {
            createDunningCycleRequest.scheduleRule.oneTimeDate = Timestamp(
                createDunningCycleRequest.scheduleRule.oneTimeDate!!.time.minus(
                    AresConstants.TIME_ZONE_DIFFENRENCE_FROM_GMT.get(
                        AresConstants.TimeZone.valueOf(createDunningCycleRequest.scheduleRule.scheduleTimeZone)
                    ) ?: throw AresException(AresError.ERR_1002, "")
                )
            )
        }

        val dunningCycleResponse = dunningCycleRepo.save(
            DunningCycle(
                id = null,
                name = createDunningCycleRequest.name,
                cycle_type = DunningCycleType.valueOf(createDunningCycleRequest.cycle_type).toString(),
                triggerType = TriggerType.valueOf(createDunningCycleRequest.triggerType).toString(),
                frequency = FREQUENCY.valueOf(createDunningCycleRequest.frequency).toString(),
                severityLevel = AresConstants.DUNNING_SEVERITY_LEVEL.get(SeverityEnum.valueOf(createDunningCycleRequest.severityLevel))!!,
                entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS[createDunningCycleRequest.filters.cogoEntityId.toString()]!!,
                filters = createDunningCycleRequest.filters,
                scheduleRule = createDunningCycleRequest.scheduleRule,
                templateId = createDunningCycleRequest.templateId,
                category = createDunningCycleRequest.category ?: DunningCategory.CYCLE.toString(),
                isActive = createDunningCycleRequest.isActive ?: true,
                deletedAt = null,
                createdBy = createDunningCycleRequest.createdBy,
                updatedBy = createDunningCycleRequest.createdBy,
                createdAt = null,
                updatedAt = null
            )
        )

        if (!createDunningCycleRequest.exceptionTradePartyDetailIds.isNullOrEmpty()) {
            var organizationTradePartyDetailResponse: GetOrganizationTradePartyDetailResponse? = null
            try {
                organizationTradePartyDetailResponse = authClient.getOrganizationTradePartyDetail(
                    GetOrganizationTradePartyDetailRequest(
                        organizationTradePartyDetailIds = createDunningCycleRequest.exceptionTradePartyDetailIds!!
                    )
                )
            } catch (e: Exception) {
            }

            val dunningCycleExceptionList: MutableList<CycleExceptions> = mutableListOf()
            if (organizationTradePartyDetailResponse != null) {
                organizationTradePartyDetailResponse.list.forEach { organizationTradePartyDetail ->
                    dunningCycleExceptionList.add(
                        CycleExceptions(
                            id = null,
                            dunningCycleId = dunningCycleResponse.id!!,
                            tradePartyDetailId = organizationTradePartyDetail.organizationTradePartDetailId!!,
                            registrationNumber = organizationTradePartyDetail.registrationNumber!!,
                            deletedAt = null,
                            createdBy = dunningCycleResponse.createdBy,
                            updatedBy = dunningCycleResponse.updatedBy,
                            createdAt = null,
                            updatedAt = null
                        )
                    )
                }
            }
            cycleExceptionRepo.saveAll(dunningCycleExceptionList)
        }
        saveAndScheduleExecution(dunningCycleResponse)
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
        return dunningCycleResponse.id!!
    }

    override suspend fun saveAndScheduleExecution(dunningCycle: DunningCycle): Long {

        val dunningCycleScheduledAt = calculateNextScheduleTime(dunningCycle.scheduleRule)

        val dunningCycleExecutionResponse = dunningExecutionRepo.save(
            DunningCycleExecution(
                id = null,
                dunningCycleId = dunningCycle.id!!,
                templateId = dunningCycle.templateId!!,
                status = CycleExecutionStatus.SCHEDULED.toString(),
                filters = dunningCycle.filters,
                entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS[dunningCycle.filters.cogoEntityId.toString()]!!,
                scheduleRule = dunningCycle.scheduleRule,
                frequency = dunningCycle.frequency,
                scheduledAt = Timestamp(dunningCycleScheduledAt.time),
                triggerType = dunningCycle.triggerType,
                deletedAt = dunningCycle.deletedAt,
                createdBy = dunningCycle.createdBy,
                updatedBy = dunningCycle.updatedBy,
                createdAt = null,
                updatedAt = null
            )
        )
        val request = ObjectMapper().writeValueAsString(
            CycleExecutionProcessReq(
                scheduleId = Hashids.encode(dunningCycleExecutionResponse.id!!)
            )
        )

        val calendar = Calendar.getInstance()
        calendar.time = dunningCycleScheduledAt
        val updatedDate = calendar.time
        rabbitMq.delay("ares.dunning.scheduler", request, updatedDate)

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
        return dunningCycleExecutionResponse.id!!
    }

    private fun pushDunningInDelay(timeToProcess: Date) {
    }

    override suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): ResponseList<CustomerOutstandingAndOnAccountResponse> {
        var serviceTypes = listOf<ServiceType>()

        request.serviceTypes?.forEach { serviceType ->
            serviceTypes = serviceTypes + getServiceType(serviceType)
        }

        var taggedOrganizationIds: List<UUID>? = listOf()
        if (! (request.organizationStakeholderIds == null)) {
            taggedOrganizationIds = organizationStakeholderRepo.listOrganizationIdBasedOnorganizationStakeholderIds(
                organizationStakeholderIds = request.organizationStakeholderIds
            )
        }

        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        val response = listOnAccountAndOutstandingBasedOnDunninCycleFilters(
            DunningCycleFilterRequest(
                query = query,
                entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS[request.cogoEntityId.toString()]!!,
                serviceTypes = serviceTypes,
                taggedOrganizationIds = taggedOrganizationIds,
                totalDueOutstanding = request.totalDueOutstanding,
                ageingStartDay = getAgeingBucketDays(AgeingBucketEnum.valueOf(request.ageingBucket!!))[0],
                ageingLastDay = getAgeingBucketDays(AgeingBucketEnum.valueOf(request.ageingBucket!!))[1],
                exceptionTradePartyDetailId = listOf(),
                pageSizeData = request.pageSize,
                pageIndexData = request.pageIndex
            )
        )

        return response
    }

    open suspend fun listOnAccountAndOutstandingBasedOnDunninCycleFilters(
        dunningCycleFilterRequest: DunningCycleFilterRequest
    ): ResponseList<CustomerOutstandingAndOnAccountResponse> {

        val serviceTypes: List<ServiceType?>? = if (
            dunningCycleFilterRequest.serviceTypes == null || dunningCycleFilterRequest.serviceTypes?.size == 0
        ) null
        else
            dunningCycleFilterRequest.serviceTypes

        val serviceTypeString: MutableList<String> = mutableListOf()
        if (!serviceTypes.isNullOrEmpty()) {
            serviceTypes.forEach { serviceType ->
                serviceTypeString.add(serviceType.toString())
            }
        }

        val customerOutstandingAndOnAccountResponses: List<CustomerOutstandingAndOnAccountResponse> =
            accountUtilizationRepo.listOnAccountAndOutstandingsBasedOnDunninCycleFilters(
                query = dunningCycleFilterRequest.query,
                totalDueOutstanding = dunningCycleFilterRequest.totalDueOutstanding,
                entityCode = dunningCycleFilterRequest.entityCode,
                serviceTypes = if (serviceTypeString.isNullOrEmpty()) null else serviceTypeString,
                ageingStartDay = dunningCycleFilterRequest.ageingStartDay,
                ageingLastDay = dunningCycleFilterRequest.ageingLastDay,
                pageSize = dunningCycleFilterRequest.pageSizeData ?: dunningCycleFilterRequest.pageSize,
                pageIndex = dunningCycleFilterRequest.pageIndexData ?: dunningCycleFilterRequest.pageIndex,
                taggedOrganizationIds = dunningCycleFilterRequest.taggedOrganizationIds,
                exceptionTradePartyDetailId = dunningCycleFilterRequest.exceptionTradePartyDetailId
            )

        val response = customerOutstandingAndOnAccountResponses

        val totalCount = accountUtilizationRepo.countOnAccountAndOutstandingsBasedOnDunninCycleFilters(
            query = dunningCycleFilterRequest.query,
            totalDueOutstanding = dunningCycleFilterRequest.totalDueOutstanding,
            entityCode = dunningCycleFilterRequest.entityCode,
            serviceTypes = serviceTypeString,
            ageingStartDay = dunningCycleFilterRequest.ageingStartDay,
            ageingLastDay = dunningCycleFilterRequest.ageingLastDay,
            taggedOrganizationIds = dunningCycleFilterRequest.taggedOrganizationIds,
            exceptionTradePartyDetailId = dunningCycleFilterRequest.exceptionTradePartyDetailId
        )

        var responseList = listOf<CustomerOutstandingAndOnAccountResponse>()

        responseList = if (dunningCycleFilterRequest.exceptionTradePartyDetailId != null) {
            val customerToRemove = dunningCycleFilterRequest.exceptionTradePartyDetailId!!.map { it }
            response.filter { customer -> customer.tradePartyDetailId !in customerToRemove }
        } else {
            response
        }

        val totalPages = Utilities.getTotalPages(totalCount, dunningCycleFilterRequest.pageSize)
        return ResponseList(
            list = responseList,
            totalPages = totalPages,
            totalRecords = totalCount,
            pageNo = dunningCycleFilterRequest.pageIndexData ?: dunningCycleFilterRequest.pageIndex,
        )
    }

    private fun getAgeingBucketDays(ageingBucketName: AgeingBucketEnum): IntArray {
        return when (ageingBucketName) {
            AgeingBucketEnum.ALL -> intArrayOf(0, 0)
            AgeingBucketEnum.AB_1_30 -> intArrayOf(0, 30)
            AgeingBucketEnum.AB_31_60 -> intArrayOf(31, 60)
            AgeingBucketEnum.AB_61_90 -> intArrayOf(61, 90)
            AgeingBucketEnum.AB_91_180 -> intArrayOf(91, 180)
            AgeingBucketEnum.AB_181_PLUS -> intArrayOf(181, 181)
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
        var creditDaysFrom: Long? = null
        var creditDaysTo: Long? = null
        if (request.creditDays != null) {
            creditDaysFrom = CREDIT_DAYS_MAPPING[request.creditDays]?.first
            creditDaysTo = CREDIT_DAYS_MAPPING[request.creditDays]?.second
        }
        if (request.segmentation != null) {
            request.segmentation = SEGMENT_MAPPING[request.segmentation]
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
        responseList.forEach {
            it.id = Hashids.encode(it.id.toLong())
        }
        val totalCount = masterExceptionRepo.listMasterExceptionTotalCount(
            q,
            request.segmentation,
            creditDaysFrom,
            creditDaysTo
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

    override suspend fun deleteOrUpdateMasterException(id: String, updatedBy: UUID, actionType: String): Boolean {
        val exceptionId = Hashids.decode(id)[0]
        masterExceptionRepo.deleteOrUpdateException(exceptionId, updatedBy, actionType)
        return true
    }

    @Transactional
    override suspend fun deleteCycle(id: String, updatedBy: UUID): Boolean {
        val dunningCycleExecution = dunningExecutionRepo.findById(Hashids.decode(id)[0])
            ?: throw AresException(AresError.ERR_1545, "")
        if (
            dunningCycleExecution.deletedAt != null
        ) {
            throw AresException(AresError.ERR_1548, "")
        }

        if (dunningCycleExecution.status == CycleExecutionStatus.SCHEDULED.toString() &&
            dunningCycleExecution.deletedAt != null
        ) {
            throw AresException(AresError.ERR_1546, "")
        }

        dunningCycleRepo.deleteCycle(dunningCycleExecution.dunningCycleId!!, updatedBy)
        dunningExecutionRepo.deleteCycleExecution(dunningCycleExecution.id!!, updatedBy)

        return true
    }

    @Transactional
    override suspend fun updateStatusDunningCycle(updateDunningCycleExecutionStatusReq: UpdateDunningCycleExecutionStatusReq): Boolean {
        val dunningCycleExecution = dunningExecutionRepo.findById(Hashids.decode(updateDunningCycleExecutionStatusReq.id)[0])
            ?: throw AresException(AresError.ERR_1545, "")

        if (CycleExecutionStatus.valueOf(dunningCycleExecution.status) == CycleExecutionStatus.SCHEDULED) {
            dunningExecutionRepo.cancelCycleExecution(
                id = dunningCycleExecution.id!!,
                updatedBy = updateDunningCycleExecutionStatusReq.updatedBy,
                updatedAt = Timestamp(System.currentTimeMillis())
            )
        }

        val dunningCycle = dunningCycleRepo.findById(dunningCycleExecution.dunningCycleId)
            ?: throw AresException(AresError.ERR_1545, "")

        dunningCycleRepo.updateStatus(
            id = dunningCycle.id!!,
            status = updateDunningCycleExecutionStatusReq.isDunningCycleActive
        )

        if (updateDunningCycleExecutionStatusReq.isDunningCycleActive) {
            val dunningCycleScheduledAt = calculateNextScheduleTime(dunningCycle.scheduleRule)

            dunningExecutionRepo.save(
                DunningCycleExecution(
                    id = null,
                    dunningCycleId = dunningCycle.id!!,
                    templateId = dunningCycle.templateId!!,
                    status = CycleExecutionStatus.SCHEDULED.toString(),
                    filters = dunningCycle.filters,
                    entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS[dunningCycle.filters.cogoEntityId.toString()]!!,
                    scheduleRule = dunningCycle.scheduleRule,
                    frequency = dunningCycle.frequency,
                    scheduledAt = Timestamp(dunningCycleScheduledAt.time),
                    triggerType = dunningCycle.triggerType,
                    deletedAt = dunningCycle.deletedAt,
                    createdBy = dunningCycle.createdBy,
                    updatedBy = dunningCycle.updatedBy,
                    createdAt = null,
                    updatedAt = null
                )
            )
        }

        return true
    }

    override suspend fun listDunningCycleExecution(request: ListDunningCycleExecutionReq): ResponseList<DunningCycleExecutionResponse> {
        val status: MutableList<String> = mutableListOf()
        if (request.cycleStatus == null || request.cycleStatus!!.size == null) {
            CycleExecutionStatus.values().forEach {
                status.add(it.toString())
            }
        } else {
            request.cycleStatus!!.forEach { it ->
                status.add(CycleExecutionStatus.valueOf(it).toString())
            }
        }

        val dunningCycleType: MutableList<String> = mutableListOf()
        if (request.dunningCycleType == null || request.dunningCycleType!!.size == 0) {
            DunningCycleType.values().forEach {
                dunningCycleType.add(it.toString())
            }
        } else {
            request.dunningCycleType!!.forEach { it ->
                dunningCycleType.add(DunningCycleType.valueOf(it).toString())
            }
        }

        var frequency: String? = null
        if (request.frequency != null) {
            frequency = DunningExecutionFrequency.valueOf(request.frequency!!).toString()
        }

        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        val response = dunningCycleRepo.listDunningCycleExecution(
            query = query,
            status = status,
            dunningCycleType = dunningCycleType,
            serviceType = request.serviceType,
            sortBy = request.sortBy,
            sortType = request.sortType,
            pageIndex = request.pageIndex,
            pageSize = request.pageSize,
            frequency = frequency
        )

        val totalCount = dunningCycleRepo.totalCountDunningCycleExecution(
            query = query,
            status = status,
            dunningCycleType = dunningCycleType,
            serviceType = request.serviceType,
            frequency = frequency
        )

        response.forEach {
            it.id = Hashids.encode(it.id?.toLong()!!)
        }

        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize!!)
        val responseList = ResponseList<DunningCycleExecutionResponse>()
        responseList.list = response
        responseList.totalRecords = totalCount
        responseList.totalPages = totalPages
        responseList.pageNo = request.pageIndex

        return responseList
    }

    @Transactional
    override suspend fun updateCycleExecution(request: UpdateCycleExecutionRequest): Long {
        val dunningCycleExecution = dunningExecutionRepo.findById(Hashids.decode(request.id)[0])
            ?: throw AresException(AresError.ERR_1545, "")

        if (dunningCycleExecution.deletedAt != null ||
            dunningCycleExecution.status != CycleExecutionStatus.SCHEDULED.toString()
        ) {
            throw AresException(AresError.ERR_1546, "")
        }

        if (
            TriggerType.valueOf(dunningCycleExecution.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(dunningCycleExecution.frequency) == FREQUENCY.MONTHLY &&
            request.scheduleRule.dayOfMonth == null &&
            request.scheduleRule.dunningExecutionFrequency.let { DunningExecutionFrequency.valueOf(it) }
            != DunningExecutionFrequency.MONTHLY
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (
            TriggerType.valueOf(dunningCycleExecution.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(dunningCycleExecution.frequency) == FREQUENCY.WEEKLY &&
            request.scheduleRule.week == null &&
            request.scheduleRule.dunningExecutionFrequency.let { DunningExecutionFrequency.valueOf(it) }
            != DunningExecutionFrequency.WEEKLY
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        dunningExecutionRepo.cancelCycleExecution(
            dunningCycleExecution.id!!,
            request.updatedBy,
            Timestamp(System.currentTimeMillis())
        )

        val dunningCycleResp: DunningCycle = dunningCycleRepo.findById(dunningCycleExecution.dunningCycleId)
            ?: throw AresException(AresError.ERR_1003, "")

        dunningCycleResp.scheduleRule = request.scheduleRule
        dunningCycleResp.updatedBy = request.updatedBy

        dunningCycleRepo.update(dunningCycleResp)

        val dunningCycleScheduledAt = calculateNextScheduleTime(dunningCycleResp.scheduleRule)

        dunningCycleExecution.id = null
        dunningCycleExecution.triggerType = TriggerType.valueOf(request.triggerType).toString()
        dunningCycleExecution.status = CycleExecutionStatus.SCHEDULED.toString()
        dunningCycleExecution.createdBy = request.updatedBy
        dunningCycleExecution.updatedBy = request.updatedBy
        dunningCycleExecution.createdAt = Timestamp(System.currentTimeMillis())
        dunningCycleExecution.updatedAt = Timestamp(System.currentTimeMillis())
        dunningCycleExecution.scheduleRule = request.scheduleRule
        dunningCycleExecution.scheduledAt = Timestamp(dunningCycleScheduledAt.time)

        return dunningExecutionRepo.save(dunningCycleExecution).id!!
    }

    override suspend fun listDistinctCreditControllers(request: ListOrganizationStakeholderRequest): List<CreditControllerResponse> {
        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        return organizationStakeholderRepo.listDistinctlistOnorganizationStakeholders(query)
    }

    override suspend fun listDunningCycles(request: ListDunningCycleReq): ResponseList<DunningCycleResponse> {
        if (request.query != null) {
            request.query = util.toQueryString(request.query)
        }

        val status: Boolean = if (request.cycleStatus == null) true else request.cycleStatus == DunningCycleStatus.ACTIVE

        val response = dunningCycleRepo.listDunningCycle(
            pageSize = request.pageSize,
            query = request.query,
            status = status,
            sortBy = request.sortBy,
            sortType = request.sortType,
            pageIndex = request.pageIndex,
        )

        val totalCount = dunningCycleRepo.totalCountDunningCycle(
            query = request.query,
            status = status
        )

        response.forEach {
            it.id = Hashids.encode(it.id.toLong())
        }

        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize)
        val responseList = ResponseList<DunningCycleResponse>()
        responseList.list = response
        responseList.totalRecords = totalCount
        responseList.totalPages = totalPages
        responseList.pageNo = request.pageIndex

        return responseList
    }

    override suspend fun overallOutstandingAndOnAccountPerTradeParty(
        request: OverallOutstandingAndOnAccountRequest
    ): ResponseList<OverallOutstandingAndOnAccountResponse> {
        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        val totalCount = accountUtilizationRepo.countOverallOutstandingAndOnAccountPerTradeParty(
            query = query,
            entityCode = request.entityCode,
            serviceTypes = request.serviceTypes,
        )

        val response = accountUtilizationRepo.overallOutstandingAndOnAccountPerTradeParty(
            pageIndex = request.pageIndex,
            pageSize = request.pageSize,
            sortBy = request.sortBy,
            sortType = request.sortType,
            entityCode = request.entityCode,
            serviceTypes = request.serviceTypes,
            query = query
        )

        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize)
        val responseList = ResponseList<OverallOutstandingAndOnAccountResponse>()
        responseList.list = response
        responseList.totalRecords = totalCount
        responseList.totalPages = totalPages
        responseList.pageNo = request.pageIndex

        return responseList
    }

    override suspend fun monthWiseStatisticsOfAccountUtilization(
        request: MonthWiseStatisticsOfAccountUtilizationReuest
    ): List<MonthWiseStatisticsOfAccountUtilizationResponse> {
        var timestamp = Timestamp(System.currentTimeMillis())
        val cal = Calendar.getInstance()

        cal.timeInMillis = timestamp.time

        if (request.viewType != "TTM") {
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)

            if (request.viewType == "FY") {
                cal.set(Calendar.YEAR, request.year!! + 1)
                cal.set(Calendar.MONTH, Calendar.MARCH)
            } else if (request.viewType == "CY") {
                cal.set(Calendar.YEAR, request.year!!)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
            }

            timestamp = Timestamp.from(cal.toInstant())
        }

        val response = accountUtilizationRepo.monthWiseStatisticsOfAccountUtilization(
            timestamp = timestamp,
            serviceTypes = null
        )

        return response
    }

    override suspend fun listSeverityLevelTemplates(): MutableMap<String, String> {
        val response: MutableMap<String, String> = mutableMapOf()

        SeverityEnum.values().forEach { severityLeve ->
            response.put(severityLeve.name, severityLeve.severity)
        }

        return response
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
                        dunningCycleId = cycleId,
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
        val response = cogoBackLowLevelClient.getTradePartyDetailsByRegistrationNumber(regNos, "list_organization_trade_parties")
//        val creditLimitDetailResponse = railsClient.getOrganizationCreditLimitDetail(
//                OrganizationCreditLimitDetailReq(
//                        orgId = response.
//                )
//        )

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
                        organizationSegment = null,
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

    override suspend fun calculateNextScheduleTime(
        scheduleRule: DunningScheduleRule
    ): Date {
        val scheduleHour = extractHourAndMinute(scheduleRule.scheduleTime).get("hour")!!
        val scheduleMinute = extractHourAndMinute(scheduleRule.scheduleTime).get("minute")!!

        val scheduleTimeStampInGMT: Timestamp = when (DunningExecutionFrequency.valueOf(scheduleRule.dunningExecutionFrequency)) {
            DunningExecutionFrequency.ONE_TIME -> calculateNextScheduleTimeForOneTime(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.DAILY -> calculateNextScheduleTimeForDaily(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.WEEKLY -> calculateScheduleTimeForWeekly(scheduleRule.week!!, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.MONTHLY -> calculateScheduleTimeForMonthly(scheduleRule.dayOfMonth!!, scheduleHour, scheduleMinute)
            else -> throw AresException(AresError.ERR_1002, "")
        }

        val actiualTimestampInRespectiveTimeZone = scheduleTimeStampInGMT.time?.minus(
            AresConstants.TIME_ZONE_DIFFENRENCE_FROM_GMT.get(
                AresConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)
            ) ?: throw AresException(AresError.ERR_1002, "")
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateFormat.format(actiualTimestampInRespectiveTimeZone)
        return dateFormat.parse(formattedDate)
    }

    open suspend fun extractHourAndMinute(time: String): Map<String, String> {
        if (time.length != 5 ||
            (time.slice(0..1).toLong() > 24 || time.slice(0..1).toLong() < 0) ||
            (time.slice(3..4).toLong() > 60) || time.slice(3..4).toLong() < 0
        ) {
            throw AresException(AresError.ERR_1549, "")
        }

        return mapOf(
            "hour" to time.slice(0..1),
            "minute" to time.slice(3..4)
        )
    }

    private suspend fun calculateNextScheduleTimeForOneTime(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val scheduleDateCal = Calendar.getInstance()

        scheduleDateCal.timeInMillis = scheduleRule.oneTimeDate!!.time

        scheduleDateCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        scheduleDateCal.set(Calendar.MINUTE, scheduleMinute.toInt())

//        val localTimestampWRTZone = System.currentTimeMillis().minus(
//            AresConstants.TIME_ZONE_DIFFENRENCE_FROM_GMT.get(
//                AresConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)
//            ) ?: throw AresException(AresError.ERR_1002, "")
//        )?.plus(AresConstants.EXTRA_TIME_TO_PROCESS_DATA_DUNNING)

        val localTimestampWRTZone = System.currentTimeMillis()?.plus(AresConstants.EXTRA_TIME_TO_PROCESS_DATA_DUNNING)

        if (scheduleDateCal.timeInMillis < localTimestampWRTZone) {
            throw AresException(AresError.ERR_1551, "")
        }

        return Timestamp(scheduleDateCal.timeInMillis)
    }

    private suspend fun calculateNextScheduleTimeForDaily(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = AresConstants.TIME_ZONE_DIFFENRENCE_FROM_GMT.get(
            AresConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)
        )?.plus(System.currentTimeMillis())!!?.plus(AresConstants.EXTRA_TIME_TO_PROCESS_DATA_DUNNING)

        if (
            todayCal.get(Calendar.HOUR_OF_DAY) > scheduleHour.toInt()
        ) {
            todayCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    private suspend fun calculateScheduleTimeForWeekly(week: DayOfWeek, scheduleHour: String, scheduleMinute: String): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = System.currentTimeMillis()

        if (
            !(
                (todayCal.get(Calendar.DAY_OF_WEEK) == (week.ordinal + 1)) &&
                    (todayCal.get(Calendar.HOUR_OF_DAY) < scheduleHour.toInt())
                )
        ) {
            while (todayCal.get(Calendar.DAY_OF_WEEK) != (week.ordinal + 1)) {
                todayCal.add(Calendar.DAY_OF_WEEK, 1)
            }
        }

        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    private suspend fun calculateScheduleTimeForMonthly(dayOfMonth: Int, scheduleHour: String, scheduleMinute: String): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = System.currentTimeMillis()

        if (todayCal.get(Calendar.DAY_OF_MONTH) > dayOfMonth ||
            (todayCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth && (todayCal.get(Calendar.HOUR_OF_DAY) > scheduleHour.toInt())) &&
            (todayCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth && (todayCal.get(Calendar.HOUR_OF_DAY) == scheduleHour.toInt() && todayCal.get(Calendar.MINUTE) > scheduleMinute.toInt()))
        ) {
            todayCal.add(Calendar.MONTH, 1)
        }

        todayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }
}
