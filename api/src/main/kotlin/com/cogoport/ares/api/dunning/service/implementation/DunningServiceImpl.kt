package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.dunning.entity.CreditController
import com.cogoport.ares.api.dunning.repository.CreditControllerRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import jakarta.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class DunningServiceImpl : DunningService {

    @Inject
    lateinit var creditControllerRepo: CreditControllerRepo

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
}
