package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.AresConstants.CREDIT_DAYS_MAPPING
import com.cogoport.ares.api.common.AresConstants.LEDGER_CURRENCY
import com.cogoport.ares.api.common.AresConstants.SEGMENT_MAPPING
import com.cogoport.ares.api.common.AresConstants.TIME_ZONE_DIFFERENCE_FROM_GMT
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.TokenTypes
import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.entity.OrganizationStakeholder
import com.cogoport.ares.api.dunning.mapper.DunningMapper
import com.cogoport.ares.api.dunning.model.DunningExceptionType
import com.cogoport.ares.api.dunning.model.SeverityEnum
import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.ListDunningCycleReq
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.repository.AresTokenRepo
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleRepo
import com.cogoport.ares.api.dunning.repository.DunningEmailAuditRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.repository.OrganizationStakeholderRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AuditRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.implementation.DefaultedBusinessPartnersServiceImpl
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
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
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.dunning.enum.DunningExecutionFrequency
import com.cogoport.ares.model.dunning.enum.FREQUENCY
import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import com.cogoport.ares.model.dunning.enum.OrganizationStakeholderType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreateUserRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilterRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.ListOrganizationStakeholderRequest
import com.cogoport.ares.model.dunning.request.MonthWiseStatisticsOfAccountUtilizationReuest
import com.cogoport.ares.model.dunning.request.OverallOutstandingAndOnAccountRequest
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.dunning.request.SyncOrgStakeholderRequest
import com.cogoport.ares.model.dunning.request.UpdateCycleExecutionRequest
import com.cogoport.ares.model.dunning.request.UpdateDunningCycleStatusReq
import com.cogoport.ares.model.dunning.request.UserInvitationRequest
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCardData
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
import java.time.Instant
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
    private val rabbitMq: RabbitmqService,
    private val aresTokenRepo: AresTokenRepo,
    private val railsClient: RailsClient,
    private val dunningEmailAuditRepo: DunningEmailAuditRepo,
    private val dunningMapper: DunningMapper,
    private val thirdPartyApiAuditService: ThirdPartyApiAuditService,
    private val businessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl,
    private val unifiedDBRepo: UnifiedDBRepo
) : DunningService {

    @Transactional
    override suspend fun syncOrgStakeholders(syncOrgStakeholderRequest: SyncOrgStakeholderRequest): Long {
        val organizationStakeholderType = OrganizationStakeholderType.valueOf(
            syncOrgStakeholderRequest.organizationStakeholderType
                .replace("_", " ").uppercase().replace(" ", "_")
        ).toString()

        var organizationStakeholder = organizationStakeholderRepo.getOrganizationStakeholdersUsingOrgId(
            organizationId = syncOrgStakeholderRequest.organizationId,
            organizationStakeholderType = organizationStakeholderType
        )

        if (organizationStakeholder == null) {

            val organizationSegment = syncOrgStakeholderRequest.organizationSegment!!
                .replace("_", " ").uppercase().replace(" ", "_")

            val organizationStakeholderEntity = OrganizationStakeholder(
                id = null,
                organizationStakeholderName = syncOrgStakeholderRequest.organizationStakeholderName!!,
                organizationStakeholderType = organizationStakeholderType,
                organizationId = syncOrgStakeholderRequest.organizationId,
                organizationSegment = OrganizationSegment.valueOf(organizationSegment).toString(),
                organizationStakeholderId = syncOrgStakeholderRequest.organizationId,
                createdBy = AresConstants.ARES_USER_ID,
                updatedBy = AresConstants.ARES_USER_ID,
                createdAt = null,
                updatedAt = null
            )
            organizationStakeholder = organizationStakeholderRepo.save(organizationStakeholderEntity)
        } else {
            var organizationSegment = organizationStakeholder.organizationSegment
            if (syncOrgStakeholderRequest.organizationSegment != null) {
                organizationSegment = OrganizationSegment.valueOf(
                    syncOrgStakeholderRequest.organizationSegment!!
                        .replace("_", " ").uppercase().replace(" ", "_")
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

        if (TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.ONE_TIME && FREQUENCY.valueOf(createDunningCycleRequest.frequency) != FREQUENCY.ONE_TIME) {
            throw AresException(AresError.ERR_1003, "")
        } else if (TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC && FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.ONE_TIME
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.MONTHLY &&
            (
                DunningExecutionFrequency.valueOf(createDunningCycleRequest.scheduleRule.dunningExecutionFrequency) != DunningExecutionFrequency.MONTHLY ||
                    createDunningCycleRequest.scheduleRule.dayOfMonth == null ||
                    createDunningCycleRequest.scheduleRule.dayOfMonth!! < 1 ||
                    createDunningCycleRequest.scheduleRule.dayOfMonth!! > AresConstants.MAX_DAY_IN_MONTH_FOR_DUNNING
                )
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (
            TriggerType.valueOf(createDunningCycleRequest.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(createDunningCycleRequest.frequency) == FREQUENCY.WEEKLY &&
            (
                DunningExecutionFrequency.valueOf(createDunningCycleRequest.scheduleRule.dunningExecutionFrequency) != DunningExecutionFrequency.WEEKLY ||
                    createDunningCycleRequest.scheduleRule.week == null
                )
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        val dunningCycleResponse = dunningCycleRepo.save(
            DunningCycle(
                id = null,
                name = createDunningCycleRequest.name,
                cycleType = DunningCycleType.valueOf(createDunningCycleRequest.cycleType).toString(),
                triggerType = TriggerType.valueOf(createDunningCycleRequest.triggerType).toString(),
                frequency = FREQUENCY.valueOf(createDunningCycleRequest.frequency).toString(),
                severityLevel = AresConstants.DUNNING_SEVERITY_LEVEL[SeverityEnum.valueOf(createDunningCycleRequest.severityLevel)]!!,
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
            val request = GetOrganizationTradePartyDetailRequest(organizationTradePartyDetailIds = createDunningCycleRequest.exceptionTradePartyDetailIds!!)
            try {
                organizationTradePartyDetailResponse = authClient.getOrganizationTradePartyDetail(request)
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(dunningCycleResponse.id!!, request.toString(), err.toString(), "list_organization_trade_party_business_finance", "dunning_cycle", "organization")
            }

            val dunningCycleExceptionList: MutableList<CycleExceptions> = mutableListOf()
            organizationTradePartyDetailResponse?.list?.forEach { organizationTradePartyDetail ->
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
                updatedAt = null,
                serviceId = null
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

    override suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): ResponseList<CustomerOutstandingAndOnAccountResponse> {
        var serviceTypes = listOf<ServiceType>()

        request.serviceTypes?.forEach { serviceType ->
            serviceTypes = serviceTypes + getServiceType(serviceType)
        }

        var taggedOrganizationIds: List<UUID>? = listOf()
        if (request.organizationStakeholderIds != null) {
            taggedOrganizationIds = organizationStakeholderRepo.listOrganizationIdBasedOnOrganizationStakeholderIds(
                organizationStakeholderIds = request.organizationStakeholderIds
            )
        }

        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        return listOnAccountAndOutstandingBasedOnDunningCycleFilters(
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
    }

    open suspend fun listOnAccountAndOutstandingBasedOnDunningCycleFilters(
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
//                serviceTypes = if (serviceTypeString.isNullOrEmpty()) null else serviceTypeString,
                ageingStartDay = dunningCycleFilterRequest.ageingStartDay,
                ageingLastDay = dunningCycleFilterRequest.ageingLastDay,
                pageSize = dunningCycleFilterRequest.pageSizeData ?: dunningCycleFilterRequest.pageSize,
                pageIndex = dunningCycleFilterRequest.pageIndexData ?: dunningCycleFilterRequest.pageIndex,
                taggedOrganizationIds = dunningCycleFilterRequest.taggedOrganizationIds,
                exceptionTradePartyDetailId = dunningCycleFilterRequest.exceptionTradePartyDetailId
            )

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

        val responseList: List<CustomerOutstandingAndOnAccountResponse> =
            if (dunningCycleFilterRequest.exceptionTradePartyDetailId != null) {
                val customerToRemove = dunningCycleFilterRequest.exceptionTradePartyDetailId!!.map { it }
                customerOutstandingAndOnAccountResponses.filter { customer -> customer.tradePartyDetailId !in customerToRemove }
            } else {
                customerOutstandingAndOnAccountResponses
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
            ServiceType.FCL_FREIGHT -> listOf(
                ServiceType.FCL_FREIGHT, ServiceType.FCL_CUSTOMS, ServiceType.FCL_FREIGHT_LOCAL,
                ServiceType.FCL_CUSTOMS, ServiceType.FCL_CFS
            )

            ServiceType.LCL_FREIGHT -> listOf(
                ServiceType.LCL_FREIGHT, ServiceType.LCL_CUSTOMS, ServiceType.LCL_CUSTOMS_FREIGHT
            )

            ServiceType.LTL_FREIGHT -> listOf(
                ServiceType.LTL_FREIGHT
            )

            ServiceType.AIR_FREIGHT -> listOf(
                ServiceType.AIR_FREIGHT, ServiceType.AIR_CUSTOMS_FREIGHT, ServiceType.AIR_CUSTOMS,
                ServiceType.AIR_FREIGHT_LOCAL, ServiceType.DOMESTIC_AIR_FREIGHT
            )

            ServiceType.FTL_FREIGHT -> listOf(
                ServiceType.FTL_FREIGHT
            )

            ServiceType.HAULAGE_FREIGHT -> listOf(
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
            request.entities,
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
            request.entities,
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
        val dunningId = Hashids.decode(id)[0]
        val isScheduledExecutionExist = dunningExecutionRepo.isisScheduledExecutionExist(dunningId) > 0
        if (isScheduledExecutionExist) {
            throw AresException(AresError.ERR_1548, "")
        } else {
            dunningCycleRepo.deleteCycle(dunningId, updatedBy)
        }
        return true
    }

    @Transactional
    override suspend fun updateStatusDunningCycle(updateDunningCycleExecutionStatusReq: UpdateDunningCycleStatusReq): Boolean {
        val dunningId = Hashids.decode(updateDunningCycleExecutionStatusReq.id)[0]
        val dunningCycles = dunningCycleRepo.findById(dunningId) ?: throw AresException(AresError.ERR_1545, "")

        dunningCycleRepo.updateStatus(
            id = dunningId,
            status = updateDunningCycleExecutionStatusReq.isDunningCycleActive
        )
        if (!updateDunningCycleExecutionStatusReq.isDunningCycleActive) {
            dunningExecutionRepo.cancelExecutions(dunningId, updateDunningCycleExecutionStatusReq.updatedBy)
        } else {
            saveAndScheduleExecution(dunningCycles)
        }
        return true
    }

    override suspend fun listDunningCycleExecution(request: ListDunningCycleExecutionReq): ResponseList<DunningCycleExecutionResponse> {
        if (request.dunningCycleId == null && request.serviceId == null) {
            throw AresException(AresError.ERR_1003, "dunning cycle id")
        }
        var dunningCycleId: Long? = null
        if (request.dunningCycleId != null) {
            dunningCycleId = Hashids.decode(request.dunningCycleId!!)[0]
        }
        val response = dunningCycleRepo.listDunningCycleExecution(
            dunningCycleId = dunningCycleId,
            serviceId = request.serviceId
        )

        val totalCount = dunningCycleRepo.totalCountDunningCycleExecution(
            dunningCycleId = dunningCycleId,
            serviceId = request.serviceId
        )

        response.forEach {
            it.id = Hashids.encode(it.id.toLong())
        }

        val totalPages = Utilities.getTotalPages(totalCount, request.pageSize)
        val responseList = ResponseList<DunningCycleExecutionResponse>()
        responseList.list = response
        responseList.totalRecords = totalCount
        responseList.totalPages = totalPages
        responseList.pageNo = request.pageIndex

        return responseList
    }

    @Transactional
    override suspend fun updateCycleExecution(request: UpdateCycleExecutionRequest): Long {
        val dunningCycle = dunningCycleRepo.findById(Hashids.decode(request.id)[0])
            ?: throw AresException(AresError.ERR_1545, "")

        if (
            TriggerType.valueOf(dunningCycle.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(dunningCycle.frequency) == FREQUENCY.MONTHLY &&
            (
                request.scheduleRule.dayOfMonth == null ||
                    request.scheduleRule.dunningExecutionFrequency.let { DunningExecutionFrequency.valueOf(it) }
                    != DunningExecutionFrequency.MONTHLY
                )
        ) {
            throw AresException(AresError.ERR_1003, "")
        }

        if (
            TriggerType.valueOf(dunningCycle.triggerType) == TriggerType.PERIODIC &&
            FREQUENCY.valueOf(dunningCycle.frequency) == FREQUENCY.WEEKLY &&
            (
                request.scheduleRule.week == null ||
                    request.scheduleRule.dunningExecutionFrequency.let { DunningExecutionFrequency.valueOf(it) }
                    != DunningExecutionFrequency.WEEKLY
                )
        ) {
            throw AresException(AresError.ERR_1003, "")
        }
        dunningExecutionRepo.cancelExecutions(
            dunningCycle.id!!,
            request.updatedBy,
        )
        dunningCycle.triggerType = request.triggerType
        dunningCycle.updatedBy = request.updatedBy
        dunningCycle.scheduleRule = request.scheduleRule
        dunningCycleRepo.update(dunningCycle)
        if (dunningCycle.isActive == true) {
            saveAndScheduleExecution(dunningCycle)
        }
        return dunningCycle.id!!
    }

    override suspend fun listDistinctCreditControllers(request: ListOrganizationStakeholderRequest): List<CreditControllerResponse> {
        var query: String? = null
        if (request.query != null)
            query = "%${request.query}%"

        return organizationStakeholderRepo.listDistinctOrganizationStakeholders(query)
    }

    override suspend fun listDunningCycles(request: ListDunningCycleReq): ResponseList<DunningCycleResponse> {
        if (request.query != null) {
            request.query = util.toQueryString(request.query)
        }

        val response = dunningCycleRepo.listDunningCycle(
            pageSize = request.pageSize,
            query = request.query,
            dunningCycleType = request.dunningCycleType,
            sortBy = request.sortBy,
            sortType = request.sortType,
            pageIndex = request.pageIndex,
        )

        val totalCount = dunningCycleRepo.totalCountDunningCycle(
            query = request.query,
            dunningCycleType = request.dunningCycleType
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
            entityCode = request.entityCodes,
            serviceTypes = request.serviceTypes,
        )

        val response = accountUtilizationRepo.overallOutstandingAndOnAccountPerTradeParty(
            pageIndex = request.pageIndex,
            pageSize = request.pageSize,
            sortBy = request.sortBy,
            sortType = request.sortType,
            entityCode = request.entityCodes,
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
            serviceTypes = request.serviceTypes,
            entityCodes = request.entityCodes
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
        if (request.exceptionType == DunningExceptionType.MASTER && request.entityCode == null) {
            throw AresException(AresError.ERR_1003, "entity code is mandatory for Master Exception Creation")
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
                        entityCode = request.entityCode!!,
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
        val extractTime = extractHourAndMinute(scheduleRule.scheduleTime)
        val scheduleHour = extractTime["hour"]!!
        val scheduleMinute = extractTime["minute"]!!

        val scheduleTimeStampInGMT: Timestamp = when (DunningExecutionFrequency.valueOf(scheduleRule.dunningExecutionFrequency)) {
            DunningExecutionFrequency.ONE_TIME -> calculateNextScheduleTimeForOneTime(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.DAILY -> calculateNextScheduleTimeForDaily(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.WEEKLY -> calculateScheduleTimeForWeekly(scheduleRule.week!!, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.MONTHLY -> calculateScheduleTimeForMonthly(scheduleRule.dayOfMonth!!, scheduleHour, scheduleMinute)
            else -> throw AresException(AresError.ERR_1002, "")
        }

        val actualTimestampInRespectiveTimeZone = scheduleTimeStampInGMT.time.minus(
            TIME_ZONE_DIFFERENCE_FROM_GMT[AresConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)]
                ?: throw AresException(AresError.ERR_1002, "")
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateFormat.format(actualTimestampInRespectiveTimeZone)
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

    private fun calculateNextScheduleTimeForOneTime(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val scheduleDateCal = Calendar.getInstance()

        scheduleDateCal.timeInMillis = System.currentTimeMillis()

        scheduleDateCal.set(Calendar.DAY_OF_MONTH, scheduleRule.oneTimeDate!!.slice(0..1).toInt())
        scheduleDateCal.set(Calendar.MONTH, (scheduleRule.oneTimeDate!!.slice(3..4).toInt()).minus(1))
        scheduleDateCal.set(Calendar.YEAR, scheduleRule.oneTimeDate!!.slice(6..9).toInt())
        scheduleDateCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        scheduleDateCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        val localTimestampWRTZone: Long = System.currentTimeMillis().plus(AresConstants.EXTRA_TIME_TO_PROCESS_DATA_DUNNING)

        if (scheduleDateCal.timeInMillis < localTimestampWRTZone) {
            throw AresException(AresError.ERR_1551, "")
        }

        return Timestamp(scheduleDateCal.timeInMillis)
    }

    private fun calculateNextScheduleTimeForDaily(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = TIME_ZONE_DIFFERENCE_FROM_GMT[AresConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)]?.plus(System.currentTimeMillis())!!

        if (todayCal.get(Calendar.HOUR_OF_DAY) > scheduleHour.toInt()) {
            todayCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    private fun calculateScheduleTimeForWeekly(week: DayOfWeek, scheduleHour: String, scheduleMinute: String): Timestamp {
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

    private fun calculateScheduleTimeForMonthly(dayOfMonth: Int, scheduleHour: String, scheduleMinute: String): Timestamp {
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

    override suspend fun createDunningPaymentLink(token: String): String {
        val tokenDetails = aresTokenRepo.findByTokens(token, TokenTypes.DUNNING_PAYMENT.name) ?: throw AresException(AresError.ERR_1002, "Link is not valid")

        if (tokenDetails.expiryTime!! <= Timestamp.from(Instant.now())) {
            throw AresException(AresError.ERR_1002, "payment link is expired")
        }
        return ""
    }

    override suspend fun createRelevantUser(request: CreateUserRequest): String? {
        val tokenDetails = aresTokenRepo.findByTokens(request.userToken, TokenTypes.RELEVANT_USER.name) ?: throw AresException(AresError.ERR_1002, "Link is not valid")

        if (tokenDetails.expiryTime!! <= Timestamp.from(Instant.now())) {
            throw AresException(AresError.ERR_1002, "user invitation link is expired")
        }
        if (tokenDetails.data?.dunningUserInviteData?.userInvitationId != null) {
            throw AresException(AresError.ERR_1002, "user already invited with this link")
        }
        val additionalData = dunningEmailAuditRepo.findById(tokenDetails.objectId)
        val orgUserDetails = dunningMapper.convertUserRequestToUserInvitationRequest(request)
        val request = UserInvitationRequest(
            performedById = additionalData?.userId.toString(),
            performedByType = "user",
            orgId = additionalData?.organizationId.toString(),
            orgUserDetails = mutableListOf(orgUserDetails)
        )

        var userInvitationId: String? = null
        try {
            userInvitationId = railsClient.createOrgUserInvitation(request)?.get("id")
        } catch (err: Exception) {
            recordFailedThirdPartyApiAudits(additionalData?.id!!, request.toString(), err.toString(), "create_organization_user_invitation", "dunning_user_invitation", "organization")
            throw err
        }
        tokenDetails.data?.dunningUserInviteData?.userInvitationId = UUID.fromString(userInvitationId)
        aresTokenRepo.update(tokenDetails)
//        aresMessagePublisher.emitUserInvitationSendPlatformNotification(additionalData) // in future we can send platform notification to kams
        return userInvitationId
    }

    override suspend fun sendMailOfAllCommunicationToTradeParty(
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq,
        isSynchronousCall: Boolean
    ): String {

        if (isSynchronousCall) {
            return "We got your request. will sent you mail on ${sendMailOfAllCommunicationToTradePartyReq.userEmail} with report."
        }

        TODO("write code to send mail")
        return ""
    }

    override suspend fun dunningCardData(entityCode: MutableList<Int>?): DunningCardData {
        val defaultedOrgIds = businessPartnersServiceImpl.listTradePartyDetailIds()
        val response = accountUtilizationRepo.getCustomerWithOutStanding(entityCode, defaultedOrgIds)
        response.ledgerCurrency = LEDGER_CURRENCY[entityCode?.get(0)] ?: "INR"
        val onAccount = unifiedDBRepo.getOnAccountAmount(entityCode, defaultedOrgIds, "AR", listOf("REC", "CTDS", "BANK", "CONTR", "ROFF", "MTCCV", "MISC", "INTER", "OPDIV", "MTC", "PAY"))
        response.totalOutstandingAmount = response.totalOutstandingAmount?.minus(onAccount ?: 0.toBigDecimal())
        response.activeCycles = dunningCycleRepo.totalCountDunningCycle(status = true)
        return response
    }

    private suspend fun recordFailedThirdPartyApiAudits(objectId: Long, request: String, response: String, apiName: String, objectName: String, apiType: String) {
        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                null,
                apiName,
                apiType,
                objectId,
                objectName,
                "500",
                request,
                response,
                false
            )
        )
    }
}
