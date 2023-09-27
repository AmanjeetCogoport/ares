package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.models.CreditControllerDetails
import com.cogoport.ares.api.common.models.EmailBankDetails
import com.cogoport.ares.api.dunning.service.interfaces.EmailService
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.api.dunning.DunningConstants.EMAIL_TEMPLATE_FOR_IRN_GENERATION
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
        var senderEmail: String? = ""
        var recieverEmail: String? = ""
        if (toUserEmail.isNotEmpty()) {
            ccEmailList = toUserEmail.subList(1, toUserEmail.size)
        }
        if(toUserEmail.isNotEmpty()){
            recieverEmail = toUserEmail[0]
        }
        if(creditControllerDetails.isNotEmpty()){
            senderEmail = creditControllerDetails[0].email
        }
//        if(toUserEmail.isEmpty()){
//            dunningEmailAuditObject.errorReason = "no user found"
//            createDunningAudit(dunningEmailAuditObject)
//        }

        val communicationRequest = CreateCommunicationRequest(
            recipientEmail = recieverEmail,
            templateName = EMAIL_TEMPLATE_FOR_IRN_GENERATION,
            senderEmail = senderEmail,
            ccEmails = ccEmailList,
            emailVariables = variables,
        )
        try {
            aresMessagePublisher.sendEmail(communicationRequest)
        } catch (err: Exception) {
            recordFailedThirdPartyApiAudits(invoiceId, communicationRequest.toString(), err.toString(), "create_communication")
        }
    }
    private suspend fun getEmailVariablesForIrnGeneration(
        bankDetails: EmailBankDetails,
        invoiceId: Long,
        creditControllerDetails: List<CreditControllerDetails>
    ): HashMap<String, String?> {
        val invoiceEmailDetails = unifiedDBNewRepository.getInvoiceDetails(invoiceId)
        return hashMapOf(
            "bank_name" to bankDetails.bankName,
            "invoice_url" to invoiceEmailDetails.invoicePdfUrl,
            "account_number" to bankDetails.accountNumber,
            "credit_controller_name" to creditControllerDetails.get(0).name,
            "credit_controller_email" to creditControllerDetails.get(0).email,
            "credit_controller_mobile_code" to creditControllerDetails.get(0).mobileCountryCode,
            "credit_controller_mobile_number" to creditControllerDetails.get(0).mobileNumber,
            "beneficiary_name" to bankDetails.beneficiaryName,
                "ifsc_code" to bankDetails.ifscCode,
                "swift_code" to bankDetails.swiftCode,
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
