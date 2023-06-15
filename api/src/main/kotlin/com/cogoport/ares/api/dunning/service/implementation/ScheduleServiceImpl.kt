package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants.DUNNING_NEW_INVOICE_GENERATION_TEMPLATE
import com.cogoport.ares.api.common.AresConstants.EXCLUDED_CREDIT_CONTROLLERS
import com.cogoport.ares.api.common.AresConstants.TAGGED_ENTITY_ID_MAPPINGS
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.DunningEmailAudit
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.response.DunningDocuments
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningEmailAuditRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID

@Singleton
class ScheduleServiceImpl(
    private val dunningExecutionRepo: DunningCycleExecutionRepo,
    private val dunningService: DunningService,
    private val masterExceptionRepo: MasterExceptionRepo,
    private val cycleExceptionRepo: CycleExceptionRepo,
    private val railsClient: RailsClient,
    private val outstandingService: OutStandingService,
    private val dunningEmailAuditRepo: DunningEmailAuditRepo,
    private val accountUtilizationRepo: AccountUtilizationRepo
) : ScheduleService {

    override suspend fun processCycleExecution(request: CycleExecutionProcessReq) {
        val executionId = Hashids.decode(request.scheduleId)[0]
        val executionDetails = dunningExecutionRepo.findById(executionId)
        if (executionDetails == null || executionDetails.deletedAt != null) {
            return
        }
        try {
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.IN_PROGRESS.name)
            fetchOrgsAndSendPaymentReminder(executionDetails)
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.COMPLETED.name)
        } catch (err: Exception) {
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.FAILED.name)
        }
        // logic for next execution creation
    }

    private suspend fun fetchOrgsAndSendPaymentReminder(executionDetails: DunningCycleExecution) {
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

    private suspend fun sendPaymentReminderToTradeParties(executionId: Long, tradePartyDetailId: UUID) {
        val cycleExecution = dunningExecutionRepo.findById(executionId)
        val entityCode = TAGGED_ENTITY_ID_MAPPINGS[cycleExecution?.filters?.cogoEntityId.toString()]
        val templateName = railsClient.listCommunicationTemplate(cycleExecution!!.templateId).list[0]["name"].toString()
        val outstandingData = outstandingService.listCustomerDetails(
            CustomerOutstandingRequest(
                tradePartyDetailId = tradePartyDetailId,
                entityCode = entityCode
            )
        )
        if (outstandingData.list.isEmpty()) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "outstanding not found")
            return
        }
        // logic to get list of not fully utilized payments on this trade party detail id
        // logic to get list of invoices
        val paymentDocuments = accountUtilizationRepo.getDocumentsForDunning(
            entityCode = entityCode!!,
            tradePartyDetailId = tradePartyDetailId,
            accType = listOf("REC")
        )
        var invoiceDocuments: List<DunningDocuments>? = null
        if (templateName == DUNNING_NEW_INVOICE_GENERATION_TEMPLATE) {
            invoiceDocuments = accountUtilizationRepo.getDocumentsForDunning(
                entityCode = entityCode,
                tradePartyDetailId = tradePartyDetailId,
                transactionDateStart = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                transactionDateEnd = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                accType = listOf("SINV", "SCN")
            )
        } else {
            invoiceDocuments = accountUtilizationRepo.getDocumentsForDunning(
                entityCode = entityCode,
                tradePartyDetailId = tradePartyDetailId,
                limit = 50,
                accType = listOf("SINV", "SCN")
            )
        }
        if (invoiceDocuments.isEmpty()) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "invoices not found")
            return
        }
        if (outstandingData.list[0]?.creditController == null || EXCLUDED_CREDIT_CONTROLLERS.contains(outstandingData.list[0]?.creditController?.id.toString())) {
        }
    }

    private suspend fun createDunningAudit(executionId: Long, tradePartyDetailId: UUID, communicationId: UUID?, isSuccess: Boolean, errorReason: String?) {
        dunningEmailAuditRepo.save(
            DunningEmailAudit(
                id = null,
                executionId = executionId,
                tradePartyDetailId = tradePartyDetailId,
                communicationId = communicationId,
                isSuccess = isSuccess,
                errorReason = errorReason,
            )
        )
    }
}
