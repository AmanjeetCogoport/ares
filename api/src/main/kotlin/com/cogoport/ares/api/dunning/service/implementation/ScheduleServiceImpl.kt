package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants.COLLECTION_ACCOUNT_EMAIL
import com.cogoport.ares.api.common.AresConstants.COLLECTION_ACCOUNT_NAME
import com.cogoport.ares.api.common.AresConstants.DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE
import com.cogoport.ares.api.common.AresConstants.DUNNING_BANK_DETAILS
import com.cogoport.ares.api.common.AresConstants.DUNNING_EXCLUDE_WORK_SCOPES
import com.cogoport.ares.api.common.AresConstants.DUNNING_NEW_INVOICE_GENERATION_TEMPLATE
import com.cogoport.ares.api.common.AresConstants.DUNNING_VALID_TEMPLATE_NAMES
import com.cogoport.ares.api.common.AresConstants.DUNNING_WORK_SCOPES
import com.cogoport.ares.api.common.AresConstants.EXCLUDED_CREDIT_CONTROLLERS
import com.cogoport.ares.api.common.AresConstants.TAGGED_ENTITY_ID_MAPPINGS
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.DunningEmailAudit
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.UserData
import com.cogoport.ares.api.dunning.model.response.DunningInvoices
import com.cogoport.ares.api.dunning.model.response.DunningPayments
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningEmailAuditRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.common.CommunicationRequest
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.SupplyAgent
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import com.cogoport.brahma.hashids.Hashids
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val accountUtilizationRepo: AccountUtilizationRepo,
    private val cogoBackLowLevelClient: CogoBackLowLevelClient
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
        var customerRequest = executionDetails.filters
        customerRequest.pageSize = 10000
        val tradePartyDetails = dunningService.getCustomersOutstandingAndOnAccount(
            customerRequest
        )
        val masterExclusionList = masterExceptionRepo.getActiveTradePartyDetailIds()
        val exclusionListForThisCycle = cycleExceptionRepo.getActiveTradePartyDetailIds(executionDetails.dunningCycleId)
        val tradeParties = tradePartyDetails.list.mapNotNull { it?.tradePartyDetailId }
        val finalTradePartyIds = tradeParties - masterExclusionList.toSet() - exclusionListForThisCycle.toSet()
        finalTradePartyIds.forEach {
            sendPaymentReminderToTradeParties(executionDetails.id!!, it)
        }
    }

    private suspend fun sendPaymentReminderToTradeParties(executionId: Long, tradePartyDetailId: UUID) {
        val cycleExecution = dunningExecutionRepo.findById(executionId)
        val entityCode = TAGGED_ENTITY_ID_MAPPINGS[cycleExecution?.filters?.cogoEntityId.toString()]
        val templateData = railsClient.listCommunicationTemplate(cycleExecution!!.templateId).list
        if (templateData.isEmpty()) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "could not get template")
            return
        }
        val templateName = templateData.firstOrNull()?.get("name").toString()
        if (!DUNNING_VALID_TEMPLATE_NAMES.contains(templateName)) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "template name is not valid")
            return
        }

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
        val outstanding = outstandingData.list[0]
        // logic to get list of not fully utilized payments on this trade party detail id
        // logic to get list of invoices
        val paymentDocuments = accountUtilizationRepo.getPaymentsForDunning(
            entityCode = entityCode!!,
            tradePartyDetailId = tradePartyDetailId
        )
        var invoiceDocuments: List<DunningInvoices>? = null
        invoiceDocuments = if (templateName == DUNNING_NEW_INVOICE_GENERATION_TEMPLATE) {
            accountUtilizationRepo.getInvoicesForDunning(
                entityCode = entityCode,
                tradePartyDetailId = tradePartyDetailId,
                transactionDateStart = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                transactionDateEnd = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                sortType = "DESC"
            )
        } else {
            accountUtilizationRepo.getInvoicesForDunning(
                entityCode = entityCode,
                tradePartyDetailId = tradePartyDetailId,
                limit = 50
            )
        }

        if (invoiceDocuments.isEmpty()) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "invoices not found")
            return
        }

        var creditControllerData = outstandingData.list[0]?.creditController
            ?: getCollectionAgencyData().takeUnless { EXCLUDED_CREDIT_CONTROLLERS.contains(it.id.toString()) }

        // From Manual Dunning user Emails come to and if they do we need to consider them only
        var userList = getUsersData(executionId, tradePartyDetailId, outstanding!!)

        var emailUsers = mutableListOf<UserData>()

        if (outstanding.tradePartyType?.contains("self") == true) {
            for (scopes in DUNNING_WORK_SCOPES) {
                emailUsers = try {
                    userList.filter { it.workScopes!!.contains(scopes) } as MutableList<UserData>
                } catch (err: Exception) {
                    mutableListOf()
                }
                if (userList.isNotEmpty()) break
            }
        }

        val tempOrgUsers = userList.filter { t ->
            t.workScopes?.toSet()?.intersect(DUNNING_EXCLUDE_WORK_SCOPES.toSet()).isNullOrEmpty() &&
                t.workScopes?.isNotEmpty() == true
        }

        if (tempOrgUsers.isNotEmpty()) {
            userList = tempOrgUsers.toMutableList()
        }
        if (emailUsers.isEmpty()) {
            emailUsers = userList
        }
        if (emailUsers.isEmpty()) {
            createDunningAudit(executionId, tradePartyDetailId, null, false, "no user found")
            return
        }
        val bankDetails = DUNNING_BANK_DETAILS[entityCode]

        val ccEmail: MutableList<String> = mutableListOf()
        outstanding.salesAgent?.email?.let { ccEmail.add(it) }

        val toUserEmail = emailUsers.firstOrNull()?.email
        userList.toSet().forEach {
            if (it.email != toUserEmail) {
                it.email?.let { it1 -> ccEmail.add(it1) }
            }
        }
        // for tagged state collectionAgency we need to alter cc and recipient

        val variables = when (templateName) {
            DUNNING_NEW_INVOICE_GENERATION_TEMPLATE -> {
                getInvoiceGenerationVariables(outstanding, invoiceDocuments, creditControllerData?.name, bankDetails!!, paymentDocuments)
            }
            DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE -> {
                getBalanceConfirmationVariables(outstanding, invoiceDocuments, creditControllerData?.name, bankDetails!!, paymentDocuments)
            }
            else -> {
                getSoaVariables(outstanding, invoiceDocuments, creditControllerData?.name, bankDetails!!, paymentDocuments)
            }
        }

        var communicationRequest = CommunicationRequest(
            recipient = creditControllerData?.email,
            type = "email",
            service = "dunning_cycle",
            serviceId = "75f0005b-e77c-48fe-94fc-bf86af1dbcf1",
            templateName = templateName,
            sender = toUserEmail,
            ccMails = ccEmail,
            organizationId = outstanding.tradePartyId,
            notifyOnBounce = true,
            replyToMessageId = getReplyToMessageId(toUserEmail!!),
            variables = variables

        )
        var communicationId: UUID? = null
        try {
            communicationId = railsClient.createCommunication(communicationRequest)
        } catch (err: Exception) {
            print(err)
        }
    }

    private fun getInvoiceGenerationVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>
    ): HashMap<String, Any?> {
        return hashMapOf(
            "customerName" to outstanding.businessName,
            "unpaid_invoices_summary" to invoiceDocuments.toString(),
            "signatory" to signatory,
            "bankDetails" to "getBankDetails",
            "payment_summary" to paymentDocuments.toString(),
            "from_date" to LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            "to_date" to LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        )
    }

    private fun getBalanceConfirmationVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>
    ): HashMap<String, Any?> {
        val openInvoiceOne = outstanding.openInvoiceAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("thirty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val onAccountOne = outstanding.onAccountAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("thirty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val openInvoiceTwo = outstanding.openInvoiceAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("sixty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val onAccountTwo = outstanding.onAccountAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("sixty")!!.ledgerAmount) ?: 0.toBigDecimal()

        return hashMapOf(
            "customerName" to outstanding.businessName,
            "ageing_bracket_I" to openInvoiceOne.plus(onAccountOne),
            "ageing_bracket_II" to openInvoiceTwo.plus(onAccountTwo),
            "ageing_bracket_III" to outstanding.openInvoiceAgeingBucket?.get("ninety")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("ninety")!!.ledgerAmount),
            "ageing_bracket_IV" to outstanding.openInvoiceAgeingBucket?.get("oneEighty")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEighty")!!.ledgerAmount),
            "ageing_bracket_V" to outstanding.openInvoiceAgeingBucket?.get("oneEightyPlus")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEightyPlus")!!.ledgerAmount),
            // ageing_bracket_VI is still pending to put here
            "total_outstanding" to outstanding.openInvoice?.ledgerAmount?.plus(outstanding.onAccount?.ledgerAmount!!),
            "unpaid_invoices_summary" to invoiceDocuments.toString(),
            "signatory" to signatory,
            "bankDetails" to bankDetails,
            "payment_summary" to paymentDocuments,
            "from_date" to LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            "to_date" to LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        )
    }

    private fun getSoaVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>
    ): HashMap<String, Any?> {
        val openInvoiceOne = outstanding.openInvoiceAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("thirty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val onAccountOne = outstanding.onAccountAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("thirty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val openInvoiceTwo = outstanding.openInvoiceAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("sixty")!!.ledgerAmount) ?: 0.toBigDecimal()
        val onAccountTwo = outstanding.onAccountAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("sixty")!!.ledgerAmount) ?: 0.toBigDecimal()

        return hashMapOf(
            "customerName" to outstanding.businessName,
            "ageing_bracket_I" to openInvoiceOne.plus(onAccountOne),
            "ageing_bracket_II" to openInvoiceTwo.plus(onAccountTwo),
            "ageing_bracket_III" to outstanding.openInvoiceAgeingBucket?.get("ninety")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("ninety")!!.ledgerAmount),
            "ageing_bracket_IV" to outstanding.openInvoiceAgeingBucket?.get("oneEighty")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEighty")!!.ledgerAmount),
            "ageing_bracket_V" to outstanding.openInvoiceAgeingBucket?.get("oneEightyPlus")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEightyPlus")!!.ledgerAmount),
            // ageing_bracket_VI is still pending to put here
            "total_outstanding" to outstanding.openInvoice?.ledgerAmount?.plus(outstanding.onAccount?.ledgerAmount!!),
            "unpaid_invoices_summary" to invoiceDocuments.toString(),
            "signatory" to signatory,
            "bankDetails" to bankDetails,
            "payment_summary" to paymentDocuments
        )
    }

    private suspend fun getUsersData(executionId: Long, tradePartyDetailId: UUID, outstanding: CustomerOutstandingDocumentResponse): MutableList<UserData> {
        var userList: List<Any>? = null
        val organizationId = UUID.fromString(outstanding.tradePartyId)
        if (outstanding.tradePartyType?.contains("self") == true) {
            var partnerData: ListOrganizationTradePartyDetailsResponse? = null
            try {
                partnerData = railsClient.listPartners(organizationId)
            } catch (err: Error) {
                createDunningAudit(executionId, tradePartyDetailId, null, false, "could not get partner data")
            }
            userList = if (partnerData?.list?.isEmpty() == false) {
                val partnerId = partnerData.list[0]["id"].toString()
                railsClient.getCpUsers(UUID.fromString(partnerId)).list
            } else {
                railsClient.listOrgUsers(organizationId).list
            }
        } else if (outstanding.tradePartyType?.contains("self") == false && outstanding.tradePartyType?.contains("paying_party") == true) {
            val tradePartyDetails = railsClient.listTradeParties(tradePartyDetailId).list
            userList = tradePartyDetails.flatMap { it["billing_addresses"] as? List<HashMap<String, Any?>> ?: emptyList() }
                .flatMap { it["organization_pocs"] as? List<HashMap<String, Any>> ?: emptyList() }
        }
        val finalUserList = mutableListOf<UserData>()
        userList?.forEach {
            val userDataJsonString = ObjectMapper().writeValueAsString(it)
            val userData: UserData = ObjectMapper().readValue(userDataJsonString, UserData::class.java)
            finalUserList.add(userData)
        }
        return finalUserList
    }

    private fun getCollectionAgencyData(): SupplyAgent {
        return SupplyAgent(
            id = null,
            name = COLLECTION_ACCOUNT_NAME,
            email = COLLECTION_ACCOUNT_EMAIL,
            mobileNumber = null,
            mobileCountryCode = null
        )
    }

    // isko bhi dekhna hai
    private suspend fun getReplyToMessageId(userEmailId: String): String? {
        val dataFromRails = railsClient.listCommunication(userEmailId).list
        if (dataFromRails.isEmpty()) {
            return null
        }
        val data = dataFromRails.firstOrNull()
        return try {
            if (JSONObject(data).get("reply_to_message_id").toString() != null) JSONObject(data).get("reply_to_message_id").toString()
            else JSONObject(JSONObject(data).get("third_party_response")).get("message_id").toString()
        } catch (err: Error) {
            null
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
