package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.payment.ServiceType
import java.util.Date

interface DunningHelperService {

    suspend fun calculateNextScheduleTime(scheduleRule: DunningScheduleRule): Date

    suspend fun listSeverityLevelTemplates(): MutableMap<String, String>

    suspend fun getAgeingBucketDays(ageingBucketName: AgeingBucketEnum): IntArray

    suspend fun getServiceType(serviceType: ServiceType): List<ServiceType>

    suspend fun sendMailOfAllCommunicationToTradeParty(
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq,
        isSynchronousCall: Boolean
    ): String
}
