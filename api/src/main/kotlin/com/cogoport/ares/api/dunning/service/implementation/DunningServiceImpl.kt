package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.dunning.entity.CreditController
import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.repository.CreditControllerRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AuditRepository
import com.cogoport.ares.model.common.AuditActionName
import com.cogoport.ares.model.common.AuditObjectType
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
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.payment.ServiceType
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
    private var dunningExecutionRepo: DunningCycleExceptionRepo,
    private var dunningExceptionRepo: DunningCycleExceptionRepo,
    private var auditRepository: AuditRepository
) : DunningService {

    @Transactional
    override suspend fun createCreditController(creditControllerRequest: CreditControllerRequest): Long {
        if (creditControllerRequest.createdBy == null) throw AresException(AresError.ERR_1003, " : created by can't be null")
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

        TODO("Create entry for cycle exception")
        TODO("Write trigger for rabbitMQ.")

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
            serviceTypes = serviceTypes + serviceType
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
}
