package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.SageBankMapping
import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import com.cogoport.ares.api.migration.model.GetOrgDetailsRequest
import com.cogoport.ares.api.migration.model.GetOrgDetailsResponse
import com.cogoport.ares.api.migration.model.OnAccountApiCommonResponseMigration
import com.cogoport.ares.api.migration.model.PaymentMigrationModel
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.TradePartyRequest
import com.cogoport.ares.api.migration.model.TradePartyResponse
import com.cogoport.ares.api.migration.repository.PaymentMigrationRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import java.math.BigDecimal
import java.util.UUID
import javax.transaction.Transactional

class PaymentMigrationImpl : PaymentMigration {

    @Inject lateinit var paymentMigrationRepository: PaymentMigrationRepository

    @Inject lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject lateinit var migrationLogService: MigrationLogService

    @Inject lateinit var cogoClient: AuthClient

    override suspend fun migratePayment(paymentRecord: PaymentRecord) {
        var paymentRequest: PaymentMigrationModel? = null
        try {
            /*FETCH ORGANIZATION DETAILS BY SAGE ORGANIZATION ID*/
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = paymentRecord.sageOrganizationId,
                    organizationType = if (paymentRecord.accMode.equals("AR")) "income" else "expense"
                )
            )
            if (response.organizationId.isNullOrEmpty()) {
                logger().info("Organization id is null, not migrating payment ${paymentRecord.paymentNum}")
                migrationLogService.saveMigrationLogs(null, null, paymentRecord.paymentNum, null, null, null, null, null, null)
                return
            }
            paymentRequest = getPaymentRequest(paymentRecord, response)

            val paymentResponse: OnAccountApiCommonResponseMigration = createPaymentEntry(paymentRequest)

            migrationLogService.saveMigrationLogs(
                paymentResponse.paymentId, paymentResponse.accUtilId, paymentRequest.paymentNumValue, paymentRequest.currency,
                paymentRequest.amount, paymentRequest.ledAmount, paymentRequest.bankPayAmount, paymentRequest.accountUtilCurrAmount, paymentRequest.accountUtilLedAmount
            )

            logger().info("Payment with paymentId ${paymentRecord.paymentNum} was successfully migrated")
        } catch (ex: Exception) {
            logger().error("Error while migrating payment with paymentId ${paymentRecord.paymentNum}")
            logger().info("******* Printing Stack trace ******** ${ex.stackTraceToString()}")
            migrationLogService.saveMigrationLogs(null, null, ex.stackTraceToString(), paymentRecord.paymentNum)
        }
    }

    private fun getPaymentRequest(paymentRecord: PaymentRecord, RorOrgDetails: GetOrgDetailsResponse): PaymentMigrationModel {

        return PaymentMigrationModel(
            id = null,
            entityCode = paymentRecord.entityCode!!,
            orgSerialId = RorOrgDetails.organizationSerialId?.toLong(),
            sageOrganizationId = paymentRecord.sageOrganizationId!!,
            organizationId = UUID.fromString(RorOrgDetails.organizationId),
            organizationName = paymentRecord.organizationName,
            accCode = paymentRecord.accCode!!,
            accMode = AccMode.valueOf(paymentRecord.accMode!!),
            signFlag = paymentRecord.signFlag!!,
            currency = paymentRecord.currency!!,
            amount = paymentRecord.currencyAmount,
            ledCurrency = paymentRecord.ledgerCurrency!!,
            ledAmount = paymentRecord.ledgerAmount,
            payMode = PayMode.valueOf(paymentRecord.paymentMode!!), // getPaymentMode(paymentRecord)?.let { PaymentModeMapping.getPayMode(it) },
            narration = paymentRecord.narration,
            transRefNumber = getUTR(paymentRecord.narration!!),
            refPaymentId = null,
            transactionDate = paymentRecord.transactionDate,
            isPosted = true,
            isDeleted = false,
            createdAt = paymentRecord.createdAt!!,
            updatedAt = paymentRecord.updatedAt!!,
            cogoAccountNo = getCogoAccountNo(paymentRecord.bankShortCode!!),
            zone = RorOrgDetails.zone?.uppercase(),
            serviceType = ServiceType.NA,
            paymentCode = PaymentCode.valueOf(paymentRecord.paymentCode!!),
            bankName = getCogoBankName(paymentRecord.bankShortCode) ?: paymentRecord.bankShortCode,
            exchangeRate = paymentRecord.exchangeRate!!,
            paymentNum = getPaymentNum(paymentRecord.paymentNum)!!,
            paymentNumValue = paymentRecord.paymentNum!!,
            bankId = getCogoBankId(paymentRecord.bankShortCode),
            accountType = AccountType.valueOf(paymentRecord.accountType!!),
            accountUtilCurrAmount = paymentRecord.accountUtilAmtCurr,
            accountUtilLedAmount = paymentRecord.accountUtilAmtLed,
            accountUtilPayCurr = paymentRecord.accountUtilPayCurr,
            accountUtilPayLed = paymentRecord.accountUtilPayLed
        )
    }

    private fun getCogoAccountNo(shortCode: String): String? {
        if (shortCode == null)
            return null
        return SageBankMapping().getBankInfoByCode(shortCode)?.cogoAccountNo
    }
    private fun getCogoBankName(shortCode: String): String? {
        if (shortCode == null)
            return null
        return SageBankMapping().getBankInfoByCode(shortCode)?.bankName
    }
    private fun getCogoBankId(shortCode: String): UUID? {
        if (shortCode == null)
            return null
        return SageBankMapping().getBankInfoByCode(shortCode)?.bankId
    }

    private fun getUTR(narration: String): String? {
        var utr: String? = null
        try {
            if (narration.contains("NEFT") || narration.contains("RTGS")) {
                utr = narration.split("/")[1]
            } else if (narration.contains("IMPS")) {
                utr = narration.split(" ")[1]
            } else if (narration.contains("CMS")) {
                utr = narration.split("/")[2]
            }
            if (utr == null || utr.isEmpty()) {
                return null
            }
            if (utr.length > 30) {
                utr = utr.substring(0, 30)
            }
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            utr = null
        }
        return utr
    }

    private fun getPaymentNum(paymentNum: String?): Long {
        var numString: String = ""

        try {
            paymentNum?.forEach { ch ->
                if (ch.isDigit())
                    numString += ch
            }
            return numString.toLong()
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        return 0
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    suspend fun createPaymentEntry(receivableRequest: PaymentMigrationModel): OnAccountApiCommonResponseMigration {
        /*SAVE PAYMENTS*/
        val payment = setPaymentEntry(receivableRequest)
        val savedPayment = paymentMigrationRepository.save(payment)
        receivableRequest.id = savedPayment.id

        val accUtilEntity = setAccountUtilizations(receivableRequest, payment)
        val accUtilRes = accountUtilizationRepository.save(accUtilEntity)

        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest)

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }
        return OnAccountApiCommonResponseMigration(paymentId = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true, accUtilId = accUtilRes.id!!)
    }

    private suspend fun setPaymentEntry(receivableRequest: PaymentMigrationModel): PaymentMigrationEntity {
        val tradePartyResponse = getTradePartyInfo(receivableRequest.organizationId.toString())
        return PaymentMigrationEntity(
            id = null,
            entityCode = receivableRequest.entityCode,
            orgSerialId = if (tradePartyResponse != null && tradePartyResponse.tradePartyDetailSerialId != null) tradePartyResponse.tradePartyDetailSerialId else null,
            sageOrganizationId = receivableRequest.sageOrganizationId,
            organizationId = if (tradePartyResponse != null && tradePartyResponse.tradePartyDetailId != null) tradePartyResponse.tradePartyDetailId else null,
            organizationName = receivableRequest.organizationName,
            accCode = receivableRequest.accCode,
            accMode = receivableRequest.accMode,
            signFlag = receivableRequest.signFlag,
            currency = receivableRequest.currency,
            amount = receivableRequest.amount,
            ledCurrency = receivableRequest.ledCurrency,
            ledAmount = receivableRequest.ledAmount,
            payMode = receivableRequest.payMode,
            narration = receivableRequest.narration,
            cogoAccountNo = receivableRequest.cogoAccountNo,
            refAccountNo = null,
            bankName = receivableRequest.bankName,
            transRefNumber = receivableRequest.transRefNumber,
            refPaymentId = null,
            transactionDate = receivableRequest.transactionDate,
            isPosted = receivableRequest.isPosted,
            isDeleted = receivableRequest.isDeleted,
            createdAt = receivableRequest.createdAt,
            updatedAt = receivableRequest.updatedAt,
            paymentCode = receivableRequest.paymentCode,
            paymentNum = receivableRequest.paymentNum,
            paymentNumValue = receivableRequest.paymentNumValue,
            exchangeRate = receivableRequest.exchangeRate,
            bankId = receivableRequest.bankId,
            tradePartyMappingId = if (tradePartyResponse != null && tradePartyResponse.mappingId != null) tradePartyResponse.mappingId else null,
            taggedOrganizationId = receivableRequest.organizationId
        )
    }

    private fun setAccountUtilizations(receivableRequest: PaymentMigrationModel, paymentEntity: PaymentMigrationEntity): AccountUtilization {
        return AccountUtilization(
            id = null,
            documentNo = receivableRequest.paymentNum,
            documentValue = receivableRequest.paymentNumValue,
            zoneCode = receivableRequest.zone,
            serviceType = ServiceType.NA.name,
            documentStatus = DocumentStatus.FINAL,
            entityCode = receivableRequest.entityCode,
            category = null,
            orgSerialId = paymentEntity.orgSerialId,
            sageOrganizationId = paymentEntity.sageOrganizationId,
            organizationId = paymentEntity.organizationId,
            organizationName = paymentEntity.organizationName,
            accMode = paymentEntity.accMode,
            accCode = paymentEntity.accCode,
            accType = receivableRequest.accountType,
            signFlag = receivableRequest.signFlag,
            currency = receivableRequest.currency,
            ledCurrency = receivableRequest.ledCurrency,
            amountCurr = receivableRequest.accountUtilCurrAmount,
            amountLoc = receivableRequest.accountUtilLedAmount,
            payCurr = receivableRequest.accountUtilPayCurr,
            payLoc = receivableRequest.accountUtilPayLed,
            dueDate = receivableRequest.transactionDate,
            transactionDate = receivableRequest.transactionDate,
            createdAt = receivableRequest.createdAt,
            updatedAt = receivableRequest.updatedAt,
            tradePartyMappingId = paymentEntity.tradePartyMappingId,
            taggedOrganizationId = paymentEntity.taggedOrganizationId,
            taxableAmount = BigDecimal.ZERO
        )
    }

    private suspend fun getTradePartyInfo(orgId: String): TradePartyResponse? {
        val request = TradePartyRequest(organizationId = orgId)
        try {
            return cogoClient.getTradePartyInfo(request)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            return null
        }
    }
}
