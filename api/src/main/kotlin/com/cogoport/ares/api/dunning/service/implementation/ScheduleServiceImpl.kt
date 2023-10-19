package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.AresConstants.ENTITY_101
import com.cogoport.ares.api.common.AresConstants.LEDGER_CURRENCY
import com.cogoport.ares.api.common.AresConstants.TAGGED_ENTITY_ID_MAPPINGS
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoCareClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.TokenObjectTypes
import com.cogoport.ares.api.common.enums.TokenTypes
import com.cogoport.ares.api.dunning.DunningConstants
import com.cogoport.ares.api.dunning.DunningConstants.COLLECTION_ACCOUNT_EMAIL
import com.cogoport.ares.api.dunning.DunningConstants.COLLECTION_ACCOUNT_NAME
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_BANK_DETAILS
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_EXCLUDE_WORK_SCOPES
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_NEW_INVOICE_GENERATION_TEMPLATE
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_SOA_MAIL_TEMPLATE
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_VALID_TEMPLATE_NAMES
import com.cogoport.ares.api.dunning.DunningConstants.DUNNING_WORK_SCOPES
import com.cogoport.ares.api.dunning.DunningConstants.EXCLUDED_CREDIT_CONTROLLERS
import com.cogoport.ares.api.dunning.DunningConstants.MAX_PAGE_SIZE
import com.cogoport.ares.api.dunning.entity.AresTokens
import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.entity.DunningEmailAudit
import com.cogoport.ares.api.dunning.model.SeverityEnum
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq
import com.cogoport.ares.api.dunning.model.request.UserData
import com.cogoport.ares.api.dunning.model.response.DunningInvoices
import com.cogoport.ares.api.dunning.model.response.DunningPayments
import com.cogoport.ares.api.dunning.repository.AresTokenRepo
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleExecutionRepo
import com.cogoport.ares.api.dunning.repository.DunningCycleRepo
import com.cogoport.ares.api.dunning.repository.DunningEmailAuditRepo
import com.cogoport.ares.api.dunning.repository.MasterExceptionRepo
import com.cogoport.ares.api.dunning.repository.OrganizationStakeholderRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.CommunicationRequest
import com.cogoport.ares.model.common.CommunicationResp
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.request.CommunicationVariables
import com.cogoport.ares.model.dunning.request.PaymentSummary
import com.cogoport.ares.model.dunning.request.TicketGenerationReq
import com.cogoport.ares.model.dunning.request.UnPaidInvoiceSummary
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.SupplyAgent
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.plutus.client.PlutusClient
import com.cogoport.plutus.model.invoice.GetUserRequest
import com.cogoport.plutus.model.invoice.GetUserResponse
import com.cogoport.plutus.model.invoice.request.IrnGenerationEmailRequest
import com.cogoport.plutus.model.invoice.response.DunningPdfs
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    private val plutusClient: PlutusClient,
    private val dunningCycleRepo: DunningCycleRepo,
    private val aresMessagePublisher: AresMessagePublisher,
    private val thirdPartyApiAuditService: ThirdPartyApiAuditService,
    private val cogoCareClient: CogoCareClient,
    private val aresTokenRepo: AresTokenRepo,
    private val organizationStakeholderRepo: OrganizationStakeholderRepo,
    private val authClient: AuthClient
) : ScheduleService {

    @Value("\${cogoport.org_base_url}")
    lateinit var orgUrl: String

    @Value("\${cogoport.partner_base_url}")
    lateinit var partnerUrl: String

    override suspend fun processCycleExecution(request: CycleExecutionProcessReq) {
        val executionId = Hashids.decode(request.scheduleId).firstOrNull() ?: return
        logger().info("dunning processing started for execution $executionId")
        val executionDetails = dunningExecutionRepo.findById(executionId)
        isValidExecution(executionDetails)
        try {
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.IN_PROGRESS.name)
            fetchOrgsAndSendPaymentReminder(executionDetails!!)
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.COMPLETED.name)
        } catch (err: Exception) {
            dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.FAILED.name)
            logger().error(err.message)
        }
        // logic for next execution creation
        saveAndScheduleNextDunning(executionDetails!!)
    }
    private fun isValidExecution(executionDetails: DunningCycleExecution?) {
        if (executionDetails == null || executionDetails.deletedAt != null || executionDetails.status != CycleExecutionStatus.SCHEDULED.name) {
            logger().info("Dunning could not be processed because of invalid execution, executionId is${executionDetails?.id}")
            return
        }
    }

    private suspend fun saveAndScheduleNextDunning(executionDetails: DunningCycleExecution) {
        val dunningCycleDetails = dunningCycleRepo.findById(executionDetails.dunningCycleId)
        if (dunningCycleDetails?.triggerType == "ONE_TIME" || dunningCycleDetails?.isActive == false || dunningCycleDetails?.deletedAt != null) {
            return
        }
        dunningService.saveAndScheduleExecution(dunningCycleDetails!!)
    }

    private suspend fun fetchOrgsAndSendPaymentReminder(executionDetails: DunningCycleExecution) {
        val customerRequest = executionDetails.filters
        customerRequest.pageSize = MAX_PAGE_SIZE
        val tradePartyDetails = dunningService.getCustomersOutstandingAndOnAccount(
            customerRequest
        )
        val masterExclusionList = masterExceptionRepo.getActiveTradePartyDetailIds()
        val exclusionListForThisCycle = cycleExceptionRepo.getActiveTradePartyDetailIds(executionDetails.dunningCycleId)
        val tradeParties = tradePartyDetails.list.mapNotNull { it?.tradePartyDetailId }
        val finalTradePartyIds = tradeParties.toSet() - masterExclusionList.toSet() - exclusionListForThisCycle.toSet()
        finalTradePartyIds.forEach {
            aresMessagePublisher.sendPaymentReminder(
                PaymentReminderReq(
                    cycleExecutionId = executionDetails.id!!,
                    tradePartyDetailId = it
                )
            )
        }
    }

    override suspend fun sendPaymentReminderToTradeParty(request: PaymentReminderReq) {
        val dunningEmailAuditObject = DunningEmailAudit(id = null)
        try {
            val executionId = request.cycleExecutionId
            val tradePartyDetailId = request.tradePartyDetailId
            logger().info("mail sending process started for execution $executionId and customer $tradePartyDetailId")
            val cycleExecution = dunningExecutionRepo.findById(executionId)
            val entityCode = TAGGED_ENTITY_ID_MAPPINGS[cycleExecution?.filters?.cogoEntityId.toString()]

            dunningEmailAuditObject.executionId = executionId
            dunningEmailAuditObject.tradePartyDetailId = tradePartyDetailId
            dunningEmailAuditObject.isSuccess = false
            val outstandingData: ResponseList<CustomerOutstandingDocumentResponse?>
            try {
                val request = CustomerOutstandingRequest(
                    tradePartyDetailId = tradePartyDetailId,
                    entityCode = entityCode.toString()
                )
                outstandingData = outstandingService.listCustomerDetails(
                    request
                )
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, request.toString(), err.toString(), "list_customer_outstanding")
                throw err
            }
            if (outstandingData.list.isEmpty()) {
                dunningEmailAuditObject.errorReason = "outstanding not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }
            val outstanding = outstandingData.list[0]
            dunningEmailAuditObject.organizationId = UUID.fromString(outstanding?.tradePartyId)
            var templateData: ArrayList<HashMap<String, Any?>>? = null
            try {
                templateData = railsClient.listCommunicationTemplate(cycleExecution!!.templateId).list
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, cycleExecution?.templateId.toString(), err.toString(), "list_communication_template")
                throw err
            }
            if (templateData.isEmpty()) {
                dunningEmailAuditObject.errorReason = "could not get template"
                createDunningAudit(dunningEmailAuditObject)
                return
            }
            val templateName = templateData.firstOrNull()?.get("name").toString()
            if (!DUNNING_VALID_TEMPLATE_NAMES.contains(templateName)) {
                dunningEmailAuditObject.errorReason = "template name is not valid"
                createDunningAudit(dunningEmailAuditObject)
                return
            }
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
                    transactionDateStart = Date.from(
                        LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    ),
                    transactionDateEnd = Date.from(
                        LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    ),
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
                dunningEmailAuditObject.errorReason = "invoices not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }
            val invoiceIds = invoiceDocuments.map { it.documentNo }

            var pdfAndSids: List<DunningPdfs>? = null

            try {
                pdfAndSids = plutusClient.getDataFromDunning(invoiceIds)
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, invoiceIds.toString(), err.toString(), "list_sids_and_pdfs_from_plutus")
                throw err
            }

            invoiceDocuments.forEach { t ->
                val data = pdfAndSids.firstOrNull { it.invoiceId == t.documentNo }
                t.pdfUrl = data?.invoicePdfUrl
                t.jobNumber = data?.jobNumber
            }

            val creditControllerData = outstandingData.list[0]?.creditController
                ?: getCollectionAgencyData().takeUnless { EXCLUDED_CREDIT_CONTROLLERS.contains(it.id.toString()) }

            // From Manual Dunning user Emails come to and if they do we need to consider them only
            var userList: MutableList<UserData>? = null
            try {
                userList = getUsersData(executionId, tradePartyDetailId, outstanding!!)
            } catch (err: Exception) {
                dunningEmailAuditObject.errorReason = "could not get userData"
                createDunningAudit(dunningEmailAuditObject)
                throw err
            }

            var emailUsers = mutableListOf<UserData>()

            if (outstanding.tradePartyType?.contains("self") == true) {
                for (scopes in DUNNING_WORK_SCOPES) {
                    emailUsers = try {
                        userList.filter { it.workScopes!!.contains(scopes) } as MutableList<UserData>
                    } catch (err: Exception) {
                        logger().error(err.message)
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
                dunningEmailAuditObject.errorReason = "no user found"
                createDunningAudit(dunningEmailAuditObject)
                throw AresException(AresError.ERR_1002, "no user found")
            }
            val bankDetails = DUNNING_BANK_DETAILS[entityCode]

            val ccEmail: MutableList<String> = mutableListOf()
            outstanding.salesAgent?.email?.let { ccEmail.add(it) }

            val toUserData = emailUsers.firstOrNull()
            val toUserEmail = toUserData?.email
            val toUserId = toUserData?.userId
            dunningEmailAuditObject.userId = UUID.fromString(toUserId)
            dunningEmailAuditObject.emailRecipients = toUserEmail
            // for tagged state collectionAgency we need to alter cc and recipient in future

            userList.toSet().forEach {
                if (it.email != toUserEmail) {
                    it.email?.let { it1 -> ccEmail.add(it1) }
                }
            }
            val severityTemplate = when (dunningCycleRepo.getSeverityTemplate(cycleExecution.dunningCycleId)) {
                1 -> SeverityEnum.LOW.severity
                2 -> SeverityEnum.MEDIUM.severity
                3 -> SeverityEnum.HIGH.severity
                else -> throw AresException(AresError.ERR_1002, "Severity is Invalid")
            }
            val userIdForToken = (toUserId ?: UUID.randomUUID()).toString()
            var addUserToken: String? = null
            if (outstanding.tradePartyType?.contains("self") == true) {
                addUserToken = Utilities.getEncodedToken(userIdForToken)
            }
            val idForPaymentToken = (outstanding.tradePartyId ?: UUID.randomUUID()).toString()
            var paymentToken: String? = null
            if (templateName == DUNNING_SOA_MAIL_TEMPLATE) {
                paymentToken = Utilities.getEncodedToken(idForPaymentToken)
            }
            val ticketTokenReq = getTicketTokenRequest(toUserData!!, outstanding.tradePartyId)

            var ticketToken: String? = null
            try {
                ticketToken = cogoCareClient.getTicketToken(ticketTokenReq)?.ticketToken
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, ticketTokenReq.toString(), err.toString(), "cogo_care_token_generation")
            }

            val variables = when (templateName) {
                DUNNING_NEW_INVOICE_GENERATION_TEMPLATE -> {
                    getInvoiceGenerationVariables(
                        outstanding,
                        invoiceDocuments,
                        creditControllerData?.name,
                        bankDetails!!,
                        paymentDocuments,
                        creditControllerData?.mobileNumber.toString(),
                        addUserToken,
                        ticketToken,
                        toUserData.isPartnerUser
                    )
                }

                DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE -> {
                    getBalanceConfirmationVariables(
                        outstanding,
                        invoiceDocuments,
                        creditControllerData?.name,
                        bankDetails!!,
                        paymentDocuments,
                        creditControllerData?.mobileNumber.toString(),
                        addUserToken,
                        ticketToken,
                        toUserData.isPartnerUser
                    )
                }

                else -> {
                    getSoaVariables(
                        outstanding,
                        invoiceDocuments,
                        creditControllerData?.name,
                        bankDetails!!,
                        paymentDocuments,
                        creditControllerData?.mobileNumber.toString(),
                        severityTemplate,
                        addUserToken,
                        ticketToken,
                        toUserData.isPartnerUser,
                        paymentToken
                    )
                }
            }
            val serviceId = UUID.randomUUID().toString()
            val communicationRequest = CommunicationRequest(
                recipient = toUserEmail,
                type = "email",
                service = "dunning_cycle_bf",
                serviceId = serviceId,
                templateName = templateName,
                sender = creditControllerData?.email,
                ccMails = ccEmail,
                organizationId = outstanding.tradePartyId,
                notifyOnBounce = true,
                replyToMessageId = getReplyToMessageId(toUserEmail!!),
                variables = variables
            )
            var communicationResponse: CommunicationResp? = null
            try {
                communicationResponse = railsClient.createCommunication(communicationRequest)
                if (communicationResponse?.id == null) {
                    throw AresException(AresError.ERR_1001, "mail could not be sent")
                }
                logger().info("mail sent to user $toUserEmail and customer $tradePartyDetailId and execution $executionId")
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, communicationRequest.toString(), err.toString(), "create_communication")
                throw err
            }
            try {
                dunningExecutionRepo.updateServiceId(executionId, serviceId)
                dunningEmailAuditObject.isSuccess = true
                dunningEmailAuditObject.communicationId = UUID.fromString(communicationResponse.id)
                val auditId = createDunningAudit(dunningEmailAuditObject)
                saveTokens(listOf(Pair(addUserToken, TokenTypes.RELEVANT_USER), Pair(paymentToken, TokenTypes.DUNNING_PAYMENT)), auditId)
            } catch (err: Exception) {
                logger().info("mail sent to user $toUserEmail and customer $tradePartyDetailId and execution $executionId but after operation could not happend with communicationId ${communicationResponse.id} because $err")
            }
        } catch (err: Exception) {
            logger().info("dunning processing failed for ${request.tradePartyDetailId} and execution id ${request.cycleExecutionId} with $err")
            dunningEmailAuditObject.communicationId = null
            dunningEmailAuditObject.isSuccess = false
            dunningEmailAuditObject.errorReason = "dunning could'nt process $err"
            createDunningAudit(dunningEmailAuditObject)
        }
    }

    private suspend fun saveTokens(tokens: List<Pair<String?, TokenTypes>>, dunningAuditId: Long) {
        val tokenObjects: MutableList<AresTokens> = mutableListOf()
        tokens.forEach {
            if (it.first != null) {
                val expiryTime = when (it.second) {
                    TokenTypes.RELEVANT_USER -> Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS))
                    TokenTypes.DUNNING_PAYMENT -> Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS))
                }
                tokenObjects.add(
                    AresTokens(
                        id = null,
                        objectId = dunningAuditId,
                        objectType = TokenObjectTypes.DUNNING,
                        tokenType = it.second,
                        token = it.first!!,
                        data = null,
                        expiryTime = expiryTime,
                        createdAt = null,
                        updatedAt = null
                    )
                )
            }
        }
        aresTokenRepo.saveAll(tokenObjects)
    }

    private fun getTicketTokenRequest(userData: UserData, orgId: String?): TicketGenerationReq {
        return TicketGenerationReq(
            name = userData.name,
            email = userData.email,
            mobileCountryCode = userData.mobileCountryCode,
            mobileNumber = userData.mobileNumber,
            source = "dunning",
            type = "client",
            status = "active",
            systemUserId = userData.userId,
            organizationId = orgId
        )
    }

    private fun getInvoiceGenerationVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>,
        contactDetails: String,
        userToken: String?,
        ticketToken: String?,
        isPartnerUser: Boolean?
    ): CommunicationVariables {
        val addUserUrl = "$orgUrl/add-dunning-relevant-user-v2/$userToken"
        val ticketUrl = "$orgUrl/create-ticket-v2/$ticketToken?source=dunning&type="
        return CommunicationVariables(
            customerName = outstanding.businessName,
            unpaidInvoiceSummary = getUnPaidInvoiceSummary(invoiceDocuments),
            signatory = signatory,
            bankDetails = bankDetails,
            paymentSummary = getPaymentSummary(paymentDocuments),
            fromDate = LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            toDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            contactDetails = contactDetails,
            addUserUrl = if (outstanding.tradePartyType?.contains("self") == true) addUserUrl else orgUrl,
            invoiceUrl = if (isPartnerUser == true) partnerUrl else orgUrl,
            ticketUrl = ticketUrl
        )
    }

    private fun getBalanceConfirmationVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>,
        contactDetails: String,
        userToken: String?,
        ticketToken: String?,
        isPartnerUser: Boolean?
    ): CommunicationVariables {
        val addUserUrl = "$orgUrl/add-dunning-relevant-user-v2/$userToken"
        val ticketUrl = "$orgUrl/create-ticket-v2/$ticketToken?source=dunning&type="

        val openInvoiceOne = outstanding.openInvoiceAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("thirty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val onAccountOne = outstanding.onAccountAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("thirty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val openInvoiceTwo = outstanding.openInvoiceAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("sixty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val onAccountTwo = outstanding.onAccountAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("sixty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val entityCode = outstanding.entityCode
        return CommunicationVariables(
            customerName = outstanding.businessName,
            ageingBucket1 = formatMoney(openInvoiceOne.plus(onAccountOne))[LEDGER_CURRENCY[entityCode]],
            ageingBucket2 = formatMoney(openInvoiceTwo.plus(onAccountTwo))[LEDGER_CURRENCY[entityCode]],
            ageingBucket3 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("ninety")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("ninety")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket4 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("oneEighty")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEighty")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket5 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("threeSixtyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("threeSixtyFive")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket6 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("threeSixtyFivePlus")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("threeSixtyFivePlus")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            totalOutstanding = formatMoney(outstanding.openInvoice?.ledgerAmount?.plus(outstanding.onAccount?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            unpaidInvoiceSummary = getUnPaidInvoiceSummary(invoiceDocuments),
            signatory = signatory,
            bankDetails = bankDetails,
            paymentSummary = getPaymentSummary(paymentDocuments),
            fromDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            toDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            onAccount = formatMoney(outstanding.onAccount?.ledgerAmount)[LEDGER_CURRENCY[entityCode]],
            contactDetails = contactDetails,
            addUserUrl = if (outstanding.tradePartyType?.contains("self") == true) addUserUrl else orgUrl,
            invoiceUrl = if (isPartnerUser == true) partnerUrl else orgUrl,
            feedbackUrl = if (outstanding.tradePartyType?.contains("self") == true) addUserUrl else orgUrl,
            ticketUrl = ticketUrl
        )
    }

    private fun getSoaVariables(
        outstanding: CustomerOutstandingDocumentResponse,
        invoiceDocuments: List<DunningInvoices>,
        signatory: String?,
        bankDetails: String,
        paymentDocuments: List<DunningPayments>,
        contactDetails: String,
        severityTemplate: String,
        userToken: String?,
        ticketToken: String?,
        isPartnerUser: Boolean?,
        paymentToken: String?,
    ): CommunicationVariables {
        val addUserUrl = "$orgUrl/add-dunning-relevant-user-v2/$userToken"
        val ticketUrl = "$orgUrl/create-ticket-v2/$ticketToken?source=dunning&type="
        val paymentUrl = "$orgUrl/dunning-payment-v2/$paymentToken"

        val openInvoiceOne = outstanding.openInvoiceAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("thirty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val onAccountOne = outstanding.onAccountAgeingBucket?.get("notDue")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("thirty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val openInvoiceTwo = outstanding.openInvoiceAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.openInvoiceAgeingBucket?.get("sixty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val onAccountTwo = outstanding.onAccountAgeingBucket?.get("fortyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("sixty")?.ledgerAmount ?: 0.toBigDecimal()) ?: 0.toBigDecimal()
        val entityCode = outstanding.entityCode
        return CommunicationVariables(
            customerName = outstanding.businessName,
            ageingBucket1 = formatMoney(openInvoiceOne.plus(onAccountOne))[LEDGER_CURRENCY[entityCode]],
            ageingBucket2 = formatMoney(openInvoiceTwo.plus(onAccountTwo))[LEDGER_CURRENCY[entityCode]],
            ageingBucket3 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("ninety")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("ninety")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket4 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("oneEighty")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("oneEighty")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket5 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("threeSixtyFive")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("threeSixtyFive")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            ageingBucket6 = formatMoney(outstanding.openInvoiceAgeingBucket?.get("threeSixtyFivePlus")?.ledgerAmount?.plus(outstanding.onAccountAgeingBucket?.get("threeSixtyFivePlus")?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            totalOutstanding = formatMoney(outstanding.openInvoice?.ledgerAmount?.plus(outstanding.onAccount?.ledgerAmount ?: 0.toBigDecimal()))[LEDGER_CURRENCY[entityCode]],
            unpaidInvoiceSummary = getUnPaidInvoiceSummary(invoiceDocuments),
            signatory = signatory,
            bankDetails = bankDetails,
            paymentSummary = getPaymentSummary(paymentDocuments),
            onAccount = formatMoney(outstanding.onAccount?.ledgerAmount)[LEDGER_CURRENCY[entityCode]],
            contactDetails = contactDetails,
            severityMail = severityTemplate,
            addUserUrl = if (outstanding.tradePartyType?.contains("self") == true) addUserUrl else orgUrl,
            invoiceUrl = if (isPartnerUser == true) partnerUrl else orgUrl,
            paymentUrl = if (outstanding.entityCode == ENTITY_101) getPaymentLink(paymentUrl) else "",
            feedbackUrl = if (outstanding.tradePartyType?.contains("self") == true) addUserUrl else orgUrl,
            ticketUrl = ticketUrl
        )
    }

    private fun getUnPaidInvoiceSummary(invoiceDocuments: List<DunningInvoices>): MutableList<UnPaidInvoiceSummary> {
        val unpaidInvoiceSummary = mutableListOf<UnPaidInvoiceSummary>()
        invoiceDocuments.forEach {
            unpaidInvoiceSummary.add(
                UnPaidInvoiceSummary(
                    sid = it.jobNumber,
                    invoiceNumber = it.documentValue,
                    pdfUrl = it.pdfUrl,
                    invoiceType = it.invoiceType,
                    grandTotal = it.amountLoc.toString(),
                    balance = (it.amountLoc - it.payLoc).toString(),
                    dueDate = it.dueDate.toString(),
                    relativeDuration = it.relativeDuration
                )
            )
        }
        return unpaidInvoiceSummary
    }

    private fun getPaymentSummary(paymentDocuments: List<DunningPayments>): MutableList<PaymentSummary> {
        val paymentSummary = mutableListOf<PaymentSummary>()
        paymentDocuments.forEach {
            paymentSummary.add(
                PaymentSummary(
                    paymentNum = it.documentValue,
                    ledAmount = it.amountLoc,
                    signFlag = it.signFlag,
                    transactionDate = it.transactionDate.toString()
                )
            )
        }
        return paymentSummary
    }

    private suspend fun getUsersData(executionId: Long, tradePartyDetailId: UUID, outstanding: CustomerOutstandingDocumentResponse): MutableList<UserData> {
        var userList: List<Any>? = null
        var isPartnerTagPresent = false
        val organizationId = UUID.fromString(outstanding.tradePartyId)
        if (outstanding.tradePartyType?.contains("self") == true) {
            var partnerData: ListOrganizationTradePartyDetailsResponse? = null
            try {
                partnerData = railsClient.listPartners(organizationId)
            } catch (err: Error) {
                recordFailedThirdPartyApiAudits(executionId, organizationId.toString(), err.toString(), "list_partners")
            }
            if (partnerData?.list?.isEmpty() == false) {
                isPartnerTagPresent = true
                val partnerId = partnerData.list[0]["id"].toString()
                try {
                    userList = railsClient.getCpUsers(UUID.fromString(partnerId)).list
                } catch (err: Exception) {
                    recordFailedThirdPartyApiAudits(executionId, partnerId, err.toString(), "get_channel_partner_users")
                    throw err
                }
            } else {
                try {
                    userList = railsClient.listOrgUsers(organizationId).list
                } catch (err: Exception) {
                    recordFailedThirdPartyApiAudits(executionId, organizationId.toString(), err.toString(), "get_org_users")
                    throw err
                }
            }
        } else if (outstanding.tradePartyType?.contains("self") == false && outstanding.tradePartyType?.contains("paying_party") == true) {
            var tradePartyDetails: ArrayList<HashMap<String, Any?>>? = null
            try {
                tradePartyDetails = railsClient.listTradeParties(tradePartyDetailId).list
                userList = tradePartyDetails.flatMap { it["billing_addresses"] as? List<HashMap<String, Any?>> ?: emptyList() }
                    .flatMap { it["organization_pocs"] as? List<HashMap<String, Any>> ?: emptyList() }
            } catch (err: Exception) {
                recordFailedThirdPartyApiAudits(executionId, organizationId.toString(), err.toString(), "list_organization_trade_parties")
                throw err
            }
        }
        val finalUserList = mutableListOf<UserData>()
        userList?.forEach {
            val userDataJsonString = ObjectMapper().writeValueAsString(it)
            val userData: UserData = ObjectMapper().readValue(userDataJsonString, UserData::class.java)
            userData.isPartnerUser = isPartnerTagPresent
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

    private suspend fun getReplyToMessageId(userEmailId: String): String? {
        val dataFromRails = railsClient.listCommunication(userEmailId).list
        if (dataFromRails.isEmpty()) {
            return null
        }
        val data = dataFromRails.firstOrNull()
        if (JSONObject(data).has("reply_to_message_id")) {
            return JSONObject(data).get("reply_to_message_id").toString()
        }
        var response: String? = null
        try {
            response = JSONObject(data).getJSONObject("third_party_response").getString("message_id")
        } catch (err: JSONException) {
            response = null
            logger().info(err.message)
        }
        return response
    }

    private fun formatMoney(amount: BigDecimal?): Map<String, String> {
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        return mapOf(
            "INR" to "INR ${DecimalFormat("#,##,##0.00").format(amount)}",
            "USD" to "USD ${DecimalFormat("#,##,##0.00").format(amount)}",
            "VND" to "VND ${DecimalFormat("#,##0.00", symbols).format(amount)}"
        )
    }

    private fun getPaymentLink(paymentUrl: String): String {
        return "<div style='text-align:center;'>To make payments click the link below." +
            "</div><br/><div style = 'border-radius: 4px;border: 1px solid #2884FB;background:#2884FB;height:24px;width:145px;display: flex;flex-direction: column;justify-content: center;align-items: center;padding: 16px 32px;gap: 10px;margin:auto;'>" +
            "<a href = $paymentUrl><span style = 'color:white;text-decoration:none;margin-left:15px;'>Make Payment</span></a></div>"
    }

    override suspend fun sendEmailForIrnGeneration(request: IrnGenerationEmailRequest) {
        val dunningEmailAuditObject = DunningEmailAudit(id = null)
        try {
            dunningEmailAuditObject.executionId = request.invoiceId
            dunningEmailAuditObject.isSuccess = false
            dunningEmailAuditObject.tradePartyDetailId = UUID.fromString(request.tradePartyDetailId)

            val stakeHoldersData = organizationStakeholderRepo.getOrganisationStakeholdersList(UUID.fromString(request.organizationId))

            val creditControllerData = stakeHoldersData.filter { it.organizationStakeholderType == "CREDIT_CONTROLLER" }

            val salesAgentData = stakeHoldersData.filter { it.organizationStakeholderType == "SALES_AGENT" }

            val salesAgentIds = salesAgentData.map { it.organizationStakeholderId.toString() }

            val creditControllerDetails = authClient.getUsers(GetUserRequest(arrayListOf(creditControllerData?.get(0).organizationStakeholderId.toString())))

            val creditController = creditControllerDetails?.get(0)

            val salesAgentDetails = authClient.getUsers(GetUserRequest(ArrayList(salesAgentIds)))

            val salesAgentEmail = salesAgentDetails?.map { it.userEmail }

            if (salesAgentEmail?.isEmpty()!!) {
                dunningEmailAuditObject.errorReason = "recipient email not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }

            if (creditController?.userEmail == null) {
                dunningEmailAuditObject.errorReason = "sender email not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }

            if (request.accountNumber == null || request.ifscCode == null) {
                dunningEmailAuditObject.errorReason = "invalid bank details"
                createDunningAudit(dunningEmailAuditObject)
                return
            }

            var ccEmailList: MutableList<String>? = mutableListOf()

            val variables = getEmailVariablesForIrnGeneration(request, creditController)

            val list = salesAgentEmail.subList(1, salesAgentEmail.size)
            ccEmailList = list.filterNotNull().toMutableList()
            val serviceId = UUID.randomUUID().toString()
            val communicationRequest = CommunicationRequest(
                recipient = salesAgentEmail[0],
                type = "email",
                service = "shipment",
                serviceId = serviceId,
                templateName = DunningConstants.EMAIL_TEMPLATE_FOR_IRN_GENERATION,
                sender = creditController.userEmail,
                ccMails = ccEmailList,
                organizationId = request.organizationId,
                notifyOnBounce = true,
                replyToMessageId = null,
                variables = variables
            )
            var communicationResponse: CommunicationResp? = null
            try {
                communicationResponse = railsClient.createCommunication(communicationRequest)
                if (communicationResponse?.id == null) {
                    throw AresException(AresError.ERR_1001, "mail could not be sent")
                }
            } catch (err: Exception) {
                dunningEmailAuditObject.errorReason = err.toString()
                createDunningAudit(dunningEmailAuditObject)
                recordFailedThirdPartyApiAudits(request.invoiceId!!, communicationRequest.toString(), err.toString(), "create_communication")
            }

            try {
                dunningEmailAuditObject.isSuccess = true
                dunningEmailAuditObject.communicationId = UUID.fromString(communicationResponse?.id)
                createDunningAudit(dunningEmailAuditObject)
            } catch (err: Exception) {
                logger().info("mail sent to user ${salesAgentEmail[0]} and customer ${request.organizationId} and invoice ${request.invoiceId} but after operation could not happend with communicationId ${communicationResponse?.id} because $err")
            }
        } catch (err: Exception) {
            dunningEmailAuditObject.communicationId = null
            dunningEmailAuditObject.isSuccess = false
            dunningEmailAuditObject.errorReason = "dunning could'nt process $err"
            createDunningAudit(dunningEmailAuditObject)
        }
    }

    private fun getEmailVariablesForIrnGeneration(
        request: IrnGenerationEmailRequest,
        creditController: GetUserResponse,
    ): CommunicationVariables {
        return CommunicationVariables(
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            creditControllerName = creditController.userName,
            creditControllerMobileNumber = creditController.mobileNumber,
            creditControllerMobileCode = creditController.mobileCountryCode,
            creditControllerEmail = creditController.userEmail,
            beneficiaryName = request.beneficiaryName,
            ifscCode = request.ifscCode,
            swiftCode = request.swiftCode,
            invoiceUrl = request.invoicePdfUrl
        )
    }

    private suspend fun createDunningAudit(dunningEmailAuditObj: DunningEmailAudit): Long {
        dunningEmailAuditObj.createdAt = null
        val dunningEmailAudit = dunningEmailAuditRepo.save(
            dunningEmailAuditObj
        )
        return dunningEmailAudit.id!!
    }

    private suspend fun recordFailedThirdPartyApiAudits(executionId: Long, request: String, response: String, apiName: String) {
        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                null,
                apiName,
                "dunning",
                executionId,
                "DUNNING_EXECUTION",
                "500",
                request,
                response,
                false
            )
        )
    }
}
