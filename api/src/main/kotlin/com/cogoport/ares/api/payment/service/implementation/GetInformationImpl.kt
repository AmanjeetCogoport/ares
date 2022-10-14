package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.repository.GetInformationRepository
import com.cogoport.ares.api.payment.service.interfaces.GetInformation
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class GetInformationImpl : GetInformation {

    @Inject
    lateinit var getCurrOutstandingRepository: GetInformationRepository

    override suspend fun getCurrOutstanding(req: List<Long>): Long {
        return getCurrOutstandingRepository.getDateDiff(req)
    }
}
