package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants.TAGGED_ENTITY_ID_MAPPINGS
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.brahma.hashids.Hashids
import io.sentry.Breadcrumb.transaction
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class ScheduleServiceImpl(
    private val dunningExecutionRepo: DunningCycleExceptionRepo,
    private val dunningService: DunningService,
    private val masterExceptionRepo: MasterExceptionRepo,
    private val cycleExceptionRepo: CycleExceptionRepo,
    private val railsClient: RailsClient,
    private val outstandingService: OutStandingService
) : ScheduleService {

    override suspend fun processCycleExecution(request: CycleExecutionProcessReq) {
        val executionId = Hashids.decode(request.scheduleId)[0]
        val executionDetails = dunningExecutionRepo.findById(executionId)
        if(executionDetails == null || executionDetails.deletedAt != null){
            return
        }
        cycleExceptionRepo.updateStatus(executionId, CycleExecutionStatus.IN_PROGRESS.name)
        try {
            cycleExceptionRepo.updateStatus(executionId, CycleExecutionStatus.IN_PROGRESS.name)
            fetchOrgsAndSendPaymentReminder(executionDetails)
            cycleExceptionRepo.updateStatus(executionId, CycleExecutionStatus.COMPLETED.name)
        } catch (err: Exception){
            cycleExceptionRepo.updateStatus(executionId, CycleExecutionStatus.FAILED.name)
        }
        // logic for next execution creation
    }

    private suspend fun fetchOrgsAndSendPaymentReminder(executionDetails: DunningCycleExecution){
        val tradePartyDetails = dunningService.getCustomersOutstandingAndOnAccount(
            executionDetails.filters
        )
        val masterExclusionList = masterExceptionRepo.getActiveTradePartyDetailIds()
        val exclusionListForThisCycle = cycleExceptionRepo.getActiveTradePartyDetailIds(executionDetails.dunningCycleId)
        val tradeParties = tradePartyDetails.map { it.tradePartyDetailId }
        val finalTradePartyIds = tradeParties - masterExclusionList.toSet() - exclusionListForThisCycle.toSet()
        finalTradePartyIds.forEach {
            sendPaymentReminderToTradeParties(executionDetails.id!!, it)
        }
    }


    private suspend fun sendPaymentReminderToTradeParties(executionId: Long,tradePartyDetailId: UUID){
        val cycleExecution  = dunningExecutionRepo.findById(executionId)
        val templateName = railsClient.listCommunicationTemplate(cycleExecution!!.templateId).list[0]["name"].toString()
        val outstandingData = outstandingService.listCustomerDetails(
            CustomerOutstandingRequest(
                tradePartyDetailId = tradePartyDetailId,
                entityCode = TAGGED_ENTITY_ID_MAPPINGS[cycleExecution.filters.cogoEntityId.toString()]
            )
        )
        if(outstandingData.list.isNullOrEmpty()){

    private suspend fun fetchOrgsAndSendPaymentReminder(executionDetails: DunningCycleExecution){
        val tradePartyDetails = dunningService.getCustomersOutstandingAndOnAccount(
            executionDetails.filters
        )
        val masterExclusionList = masterExceptionRepo.getActiveTradePartyDetailIds()
        val exclusionListForThisCycle = cycleExceptionRepo.getActiveTradePartyDetailIds(executionDetails.dunningCycleId)
        val tradeParties = tradePartyDetails.map { it.tradePartyDetailId }
        val finalTradePartyIds = tradeParties - masterExclusionList.toSet() - exclusionListForThisCycle.toSet()
        finalTradePartyIds.forEach {
            sendPaymentReminderToTradeParties(executionDetails.id!!, it)
        }
    }


    private suspend fun sendPaymentReminderToTradeParties(executionId: Long,tradePartyDetailId: UUID){
        val cycleExecution  = dunningExecutionRepo.findById(executionId)
        val templateName = railsClient.listCommunicationTemplate(cycleExecution!!.templateId).list[0]["name"].toString()










    }


}