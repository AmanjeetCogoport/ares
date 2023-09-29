package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.models.EmailDetails
import com.cogoport.ares.api.dunning.DunningConstants.EMAIL_TEMPLATE_FOR_IRN_GENERATION
import com.cogoport.ares.api.dunning.entity.DunningEmailAudit
import com.cogoport.ares.api.dunning.repository.DunningEmailAuditRepo
import com.cogoport.ares.api.dunning.service.interfaces.EmailService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.CommunicationRequest
import com.cogoport.ares.model.common.CommunicationResp
import com.cogoport.ares.model.dunning.request.CommunicationVariables
import jakarta.inject.Inject
import java.util.UUID
// import java.util.*

class EmailServiceImpl(
    private val thirdPartyApiAuditService: ThirdPartyApiAuditService,
    private val dunningEmailAuditRepo: DunningEmailAuditRepo,
    private val railsClient: RailsClient
) : EmailService {
    @Inject lateinit var unifiedDBNewRepository: UnifiedDBNewRepository
    override suspend fun sendEmailForIrnGeneration(invoiceId: Long) {
        val dunningEmailAuditObject = DunningEmailAudit(id = null)
        val emailDetails = unifiedDBNewRepository.getEmailDataForIrnGeneration(invoiceId)
        try {
            dunningEmailAuditObject.executionId = invoiceId
            dunningEmailAuditObject.isSuccess = false
            dunningEmailAuditObject.tradePartyDetailId = UUID.fromString(emailDetails.organizationId)

            if (emailDetails?.customerEmail?.isEmpty()!!) {
                dunningEmailAuditObject.errorReason = "recipient email not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }

            if (emailDetails?.creditControllerDetails?.isEmpty()!! || emailDetails?.creditControllerDetails?.get(0)?.creditControllerEmail == null) {
                dunningEmailAuditObject.errorReason = "sender email not found"
                createDunningAudit(dunningEmailAuditObject)
                return
            }

            var ccEmailList: MutableList<String>? = mutableListOf()

            val variables = getEmailVariablesForIrnGeneration(emailDetails)

            if (emailDetails.customerEmail!!.isNotEmpty()) {
                ccEmailList = emailDetails.customerEmail!!.subList(1, emailDetails.customerEmail!!.size)
            }

            val serviceId = UUID.randomUUID().toString()
            val communicationRequest = CommunicationRequest(
                recipient = emailDetails.customerEmail!![0],
                type = "email",
                service = "irn_generation_mail",
                serviceId = serviceId,
                templateName = EMAIL_TEMPLATE_FOR_IRN_GENERATION,
                sender = emailDetails.creditControllerDetails!!.get(0).creditControllerEmail,
                ccMails = ccEmailList,
                organizationId = emailDetails?.organizationId,
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
                recordFailedThirdPartyApiAudits(invoiceId, communicationRequest.toString(), err.toString(), "create_communication")
            }

            try {
                dunningEmailAuditObject.isSuccess = true
                dunningEmailAuditObject.communicationId = UUID.fromString(communicationResponse?.id)
                createDunningAudit(dunningEmailAuditObject)
            } catch (err: Exception) {
                logger().info("mail sent to user ${emailDetails.customerEmail!![0]} and customer ${emailDetails?.organizationId} and invoice $invoiceId but after operation could not happend with communicationId ${communicationResponse?.id} because $err")
            }
        } catch (err: Exception) {
            logger().info("dunning processing failed for ${emailDetails.customerEmail!![0]} and invoice id $invoiceId with $err")
            dunningEmailAuditObject.communicationId = null
            dunningEmailAuditObject.isSuccess = false
            dunningEmailAuditObject.errorReason = "dunning could'nt process $err"
            createDunningAudit(dunningEmailAuditObject)
        }
    }
    private fun getEmailVariablesForIrnGeneration(
        emailDetails: EmailDetails,
    ): CommunicationVariables {
        return CommunicationVariables(
            bankName = emailDetails.bankName,
            accountNumber = emailDetails.accountNumber,
            creditControllerName = emailDetails.creditControllerDetails?.get(0)?.creditControllerName,
            creditControllerMobileNumber = emailDetails.creditControllerDetails?.get(0)?.creditControllerMobileNumber,
            creditControllerMobileCode = emailDetails.creditControllerDetails?.get(0)?.creditControllerMobileCode,
            creditControllerEmail = emailDetails.creditControllerDetails?.get(0)?.creditControllerEmail,
            beneficiaryName = emailDetails.beneficiaryName,
            ifscCode = emailDetails.ifscCode,
            swiftCode = emailDetails.swiftCode,
            invoiceUrl = emailDetails.invoicePdfUrl
        )
    }
    private suspend fun recordFailedThirdPartyApiAudits(executionId: Long, request: String, response: String, apiName: String) {
        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                null,
                apiName,
                "irn_generation_email",
                executionId,
                "IRN_GENERATION_EMAIL",
                "500",
                request,
                response,
                false
            )
        )
    }
    private suspend fun createDunningAudit(dunningEmailAuditObj: DunningEmailAudit): Long {
        dunningEmailAuditObj.createdAt = null
        val dunningEmailAudit = dunningEmailAuditRepo.save(
            dunningEmailAuditObj
        )
        return dunningEmailAudit.id!!
    }
}
