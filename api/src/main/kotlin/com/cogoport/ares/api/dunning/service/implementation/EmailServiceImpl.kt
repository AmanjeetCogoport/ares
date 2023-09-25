package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.models.BankDetails
import com.cogoport.ares.api.common.models.CreditControllerDetails
import com.cogoport.ares.api.dunning.service.interfaces.EmailService
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.model.common.CommunicationRequest
import com.cogoport.ares.model.common.CommunicationResp
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.dunning.request.EmailVariables
import jakarta.inject.Inject
import java.util.*

class EmailServiceImpl(
//        private val railsClient: RailsClient
        private val thirdPartyApiAuditService: ThirdPartyApiAuditService
): EmailService {
    @Inject lateinit var unifiedDBNewRepository: UnifiedDBNewRepository
    @Inject lateinit var aresMessagePublisher: AresMessagePublisher
    override suspend fun sendEmailForIrnGeneration(invoiceId: Long) {
            val bankDetails = unifiedDBNewRepository.getBankDetails(invoiceId)
            val toUserEmail = unifiedDBNewRepository.getSalesAgentEmail(invoiceId)
            val creditControllerDetails = unifiedDBNewRepository.getCreditControllerEmail(invoiceId)
            val orgID = unifiedDBNewRepository.getOrganisationId(invoiceId)
            var ccEmailList: MutableList<String>? = mutableListOf()
            val variables = getEmailVariablesForIrnGeneration(bankDetails, invoiceId, creditControllerDetails)
            if (!toUserEmail.isEmpty()) {
                ccEmailList = toUserEmail.subList(1, toUserEmail.size)
            }

//            val serviceId = UUID.randomUUID().toString()
            val communicationRequest = CreateCommunicationRequest(
                    recipientEmail = toUserEmail.get(0),
//                    type = "email",
//                    service = "dunning",
//                    serviceId = serviceId,
                    templateName = "send_mail_for_irn_generation_01",
                    senderEmail = creditControllerDetails.get(0).email,
                    ccEmails = ccEmailList,
//                    organizationId = orgID.toString(),
//                    notifyOnBounce = true,
                    emailVariables = variables,
                    performedByUserId = null,
                    performedByUserName = null,
//                    replyToMessageId = null,
            )
            var communicationResponse: CommunicationResp? = null
            try {
               aresMessagePublisher.sendEmail(communicationRequest)
            }catch (err: Exception){
                recordFailedThirdPartyApiAudits(0, communicationRequest.toString(), err.toString(), "create_communication")
                throw err
            }
    }
    private suspend fun getEmailVariablesForIrnGeneration(
            bankDetails: BankDetails,
            invoiceId: Long,
            creditControllerDetails: List<CreditControllerDetails>
    ): HashMap<String, String?> {
        val invoiceEmailDetails = unifiedDBNewRepository.getInvoiceDetails(invoiceId)
        return hashMapOf(
                "bankName" to bankDetails.bankName,
                "invoiceUrl" to invoiceEmailDetails.invoiceUrl,
                "accountNumber" to bankDetails.accountNumber,
                "creditControllerName" to creditControllerDetails.get(0).name,
                "creditControllerEmail" to creditControllerDetails.get(0).email,
                "creditControllerMobileCode" to creditControllerDetails.get(0).mobileCountryCode,
                "creditControllerMobileNumber" to creditControllerDetails.get(0).mobileNumber
        )
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

//    private suspend fun getEmailVariablesforIrnGeneration(bankDetails: BankDetails, invoiceId: Long,creditControllerDetails: List<CreditControllerDetails> ): HashMap<String, String?> {
//            val invoiceEmailDetails = unifiedDBNewRepository.getInvoiceDetails(invoiceId)
//            return HashMap<String, String?>() = hashMapOf(
//                    "bankName" to bankDetails.bankName,
//                    "invoiceUrl" to invoiceEmailDetails.invoiceUrl,
//                    "accountNumber" to bankDetails.accountNumber,
//                    "creditControllerName" to creditControllerDetails.get(0).name,
//                    "creditControllerEmail" to creditControllerDetails.get(0).email,
//                    "creditControllerMobileCode" to creditControllerDetails.get(0).mobileCountryCode,
//                    "creditControllerMobileNumber" to creditControllerDetails.get(0).mobileNumber
////                bankName = bankDetails.bankName,
////                    invoiceUrl = invoiceEmailDetails.invoiceUrl,
////                    accountNumber = bankDetails.accountNumber,
//////                    accountName = bankDetails.a
////                    creditControllerName = creditControllerDetails.get(0).name,
////                    creditControllerEmail = creditControllerDetails.get(0).email,
////                    creditControllerMobileCode = creditControllerDetails.get(0).mobileCountryCode,
////                    creditControllerMobileNumber = creditControllerDetails.get(0).mobileNumber,
//            )
//    }
}