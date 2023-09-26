package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.models.CreditControllerDetails
import com.cogoport.ares.api.common.models.EmailBankDetails
import com.cogoport.ares.api.dunning.service.interfaces.EmailService
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.model.common.CreateCommunicationRequest
import jakarta.inject.Inject

class EmailServiceImpl(
    private val thirdPartyApiAuditService: ThirdPartyApiAuditService
) : EmailService {
    @Inject lateinit var unifiedDBNewRepository: UnifiedDBNewRepository
    @Inject lateinit var aresMessagePublisher: AresMessagePublisher
    override suspend fun sendEmailForIrnGeneration(invoiceId: Long) {
        val bankDetails = unifiedDBNewRepository.getBankDetails(invoiceId)
        val toUserEmail = unifiedDBNewRepository.getSalesAgentEmail(invoiceId)
        val creditControllerDetails = unifiedDBNewRepository.getCreditControllerEmail(invoiceId)
        var ccEmailList: MutableList<String> = mutableListOf()
        val variables = getEmailVariablesForIrnGeneration(bankDetails, invoiceId, creditControllerDetails)
        if (!toUserEmail.isEmpty()) {
            ccEmailList = toUserEmail.subList(1, toUserEmail.size)
        }

        val communicationRequest = CreateCommunicationRequest(
            recipientEmail = toUserEmail.get(0),
            templateName = "send_mail_for_irn_generation_01",
            senderEmail = creditControllerDetails.get(0).email,
            ccEmails = ccEmailList,
            emailVariables = variables,
        )
        try {
            aresMessagePublisher.sendEmail(communicationRequest)
        } catch (err: Exception) {
            recordFailedThirdPartyApiAudits(invoiceId, communicationRequest.toString(), err.toString(), "create_communication")
            throw err
        }
    }
    private suspend fun getEmailVariablesForIrnGeneration(
        bankDetails: EmailBankDetails,
        invoiceId: Long,
        creditControllerDetails: List<CreditControllerDetails>
    ): HashMap<String, String?> {
        val invoiceEmailDetails = unifiedDBNewRepository.getInvoiceDetails(invoiceId)
        return hashMapOf(
            "bankName" to bankDetails.bankName,
            "invoiceUrl" to invoiceEmailDetails.invoicePdfUrl,
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
                "irn_generation_email",
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
