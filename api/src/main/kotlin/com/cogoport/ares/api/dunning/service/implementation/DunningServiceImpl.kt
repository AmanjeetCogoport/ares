package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.dunning.entity.CreditController
import com.cogoport.ares.api.dunning.repository.CreditControllerRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilterRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class DunningServiceImpl : DunningService {

    @Inject
    lateinit var creditControllerRepo: CreditControllerRepo

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

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
        TODO("")
    }

    override suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): List<CustomerOutstandingAndOnAccountResponse> {
        var serviceTypes = listOf<ServiceType>()

        request.serviceTypes?.forEach { serviceType ->
            serviceTypes = serviceTypes + serviceType
        }

        var taggedOrganizationIds = listOf<UUID>() // TODO("Need to add")
        var tradePartyDetailsIds = listOf<UUID>() // TODO("Need to add")

        val response = listOnAccountAndOutstandingsBasedOnDunninCycleFilters(
            DunningCycleFilterRequest(
                entityCode = AresConstants.TAGGED_ENTITY_ID_MAPPINGS.get(request.cogoEntityId.toString())!!,
                serviceTypes = serviceTypes,
                totalDueOutstanding = request.totalDueOutstanding,
                ageingStartDay = getAgeingBucketDays(request.ageingBucket).get(0),
                ageingLastDay = getAgeingBucketDays(request.ageingBucket).get(1),
                taggedOrganizationIds = taggedOrganizationIds,
                tradePartyDetailsIds = tradePartyDetailsIds
            )
        )

        return response
    }

    open suspend fun listOnAccountAndOutstandingsBasedOnDunninCycleFilters(
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
                taggedOrganizationIds = dunningCycleFilterRequest.taggedOrganizationIds,
                tradePartyDetailsIds = dunningCycleFilterRequest.tradePartyDetailsIds,
                tradePartyDetailsId = if (dunningCycleFilterRequest.tradePartyDetailsIds.isEmpty())
                    null
                else
                    dunningCycleFilterRequest.tradePartyDetailsIds[0]
            )

        val response = customerOutstandingAndOnAccountResponses.filter { customerOutstandingAndOnAccountResponse ->
            customerOutstandingAndOnAccountResponse.outstandingAmount > dunningCycleFilterRequest.totalDueOutstanding
        }

        response.forEach { r ->
            r.outstandingAmount = r.outstandingAmount + r.onAccountAmount
        }

        return response
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
