package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.MigrationConstants
import com.cogoport.ares.api.migration.constants.PaymentModeMapping
import com.cogoport.ares.api.migration.mapper.PaymentMapper
import com.cogoport.ares.api.migration.model.GetOrgDetailsRequest
import com.cogoport.ares.api.migration.model.GetOrgDetailsResponse
import com.cogoport.ares.api.migration.model.OnAccountApiCommonResponseMigration
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.repository.PaymentMigrationRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.ServiceType
import jakarta.inject.Inject
import java.math.BigDecimal
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.transaction.Transactional

class PaymentMigrationImpl : PaymentMigration {

    @Inject lateinit var paymentMapper: PaymentMapper

    @Inject lateinit var paymentMigrationRepository: PaymentMigrationRepository

    @Inject lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    @Inject lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject lateinit var migrationLogService: MigrationLogService

    @Inject lateinit var cogoClient: AuthClient

    override suspend fun migratePayment(paymentRecord: PaymentRecord) {
        var paymentRequest: com.cogoport.ares.api.migration.model.PaymentMigration? = null
        try {
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = paymentRecord.sageOrganizationId,
                    organizationType = "income",
                )
            )
            if (response.organizationId.isNullOrEmpty()) {
                logger().info("Organization id is null, not migrating payment ${getPaymentNum(paymentRecord.paymentNum)}")
                migrationLogService.saveMigrationLogs(null, null, getPaymentNum(paymentRecord.paymentNum))
                return
            }
            if (paymentRecord.paymentNum.isNullOrEmpty()) {
                // logger().info("Organization id is null, not migrating payment ${getPaymentNum(paymentRecord.paymentNum)}")
                migrationLogService.saveMigrationLogs(null, null, "ORG", null)
                return
            }
            paymentRequest = getPaymentRequest(paymentRecord, response)
            val paymentResponse: OnAccountApiCommonResponseMigration = createPaymentEntry(paymentRequest)
            migrationLogService.saveMigrationLogs(paymentResponse.paymentId, paymentResponse.accUtilId, paymentRequest.paymentNum!!)
            logger().info("Payment with paymentId ${paymentRecord.paymentNumValue} was successfully migrated")
        } catch (ex: Exception) {
            logger().error("Error while migrating payment with paymentId ${paymentRecord.paymentNumValue}")
            logger().info("******* Printing Stack trace ******** ${ex.stackTraceToString()}")
            migrationLogService.saveMigrationLogs(null, null, ex.stackTraceToString(), getPaymentNum(paymentRecord.paymentNum))
        }
    }

    private fun getPaymentRequest(paymentRecord: PaymentRecord, RorOrgDetails: GetOrgDetailsResponse): com.cogoport.ares.api.migration.model.PaymentMigration {
        return com.cogoport.ares.api.migration.model.PaymentMigration(
            id = null,
            entityType = paymentRecord.entityCode,
            fileId = 12345, // NEED TO CHANGE
            orgSerialId = RorOrgDetails.organizationSerialId?.toLong(), // NEED TO CHANGE
            sageOrganizationId = paymentRecord.sageOrganizationId,
            organizationId = UUID.fromString(RorOrgDetails.organizationId),
            organizationName = paymentRecord.organizationName,
            accCode = paymentRecord.accCode,
            accMode = AccMode.AR,
            signFlag = paymentRecord.signFlag,
            currency = paymentRecord.currency,
            amount = paymentRecord.amount,
            ledCurrency = paymentRecord.ledCurrency,
            ledAmount = paymentRecord.ledAmount,
            payMode = getPaymentMode(paymentRecord)?.let { PaymentModeMapping.getPayMode(it) },
            remarks = paymentRecord.narration,
            utr = getUTR(paymentRecord.narration!!),
            refPaymentId = null, // NEED TO CHANGE
            transactionDate = paymentRecord.transactionDate,
            isPosted = paymentRecord.isPosted,
            isDeleted = paymentRecord.isDeleted,
            createdAt = paymentRecord.createdAt,
            createdBy = MigrationConstants.createdUpdatedBy.toString(),
            updatedAt = paymentRecord.updatedAt,
            bankAccountNumber = "12345", // NEED TO CHANGE
            zone = RorOrgDetails.zone?.toUpperCase(),
            serviceType = ServiceType.NA,
            paymentCode = PaymentCode.valueOf(paymentRecord.paymentCode!!),
            paymentDate = paymentRecord.transactionDate.toString(),
            uploadedBy = MigrationConstants.createdUpdatedBy.toString(),
            bankName = paymentRecord.bankName,
            exchangeRate = paymentRecord.exchangeRate,
            paymentNum = getPaymentNum(paymentRecord.paymentNum),
            paymentNumValue = paymentRecord.paymentNumValue,
            bankId = paymentRecord.bankId
        )
    }

    private fun getPaymentMode(paymentRecord: PaymentRecord): String? {
        if (paymentRecord.narration?.contains("NEFT") == true) {
            return "NEFT"
        } else if (paymentRecord.narration?.contains("IMPS") == true) {
            return "IMPS"
        } else if (paymentRecord.narration?.contains("RTGS") == true) {
            return "RTGS"
        }
        return paymentRecord.paymentMode
    }

    private fun getUTR(narration: String): String? {
        var utr: String? = null
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
        return utr
    }

    private fun getPaymentNum(paymentNum: String?): Long? {
        if (paymentNum.isNullOrEmpty()) return null
        val matcher: Matcher = Pattern.compile("\\d+").matcher(paymentNum)
        matcher.find()
        return Integer.valueOf(matcher.group()).toLong()
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    suspend fun createPaymentEntry(receivableRequest: com.cogoport.ares.api.migration.model.PaymentMigration): OnAccountApiCommonResponseMigration {
        // val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        // val filterDateFromTs = Timestamp(dateFormat.parse(receivableRequest.paymentDate).time)
        // receivableRequest.transactionDate = filterDateFromTs
        // receivableRequest.serviceType = ServiceType.NA
        // receivableRequest.accMode = AccMode.AR
        // receivableRequest.signFlag = SignSuffix.REC.sign

        // setPaymentAmounts(receivableRequest)
        // setOrganizations(receivableRequest)

        val payment = paymentMapper.convertToEntity(receivableRequest)
        // setPaymentEntity(payment)
        // payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
        // payment.paymentNumValue = SequenceSuffix.RECEIVED.prefix + payment.paymentNum

        val savedPayment = paymentMigrationRepository.save(payment)

        receivableRequest.id = savedPayment.id
        // receivableRequest.isPosted = false
        // receivableRequest.isDeleted = false
        // receivableRequest.paymentNum = payment.paymentNum
        // receivableRequest.paymentNumValue = payment.paymentNumValue
        // receivableRequest.accCode = payment.accCode
        // receivableRequest.paymentCode = payment.paymentCode

        // val accUtilizationModel: AccUtilizationRequest = accUtilizationToPaymentConverter.convertEntityToModel(payment)

        val accUtilizationModel: AccUtilizationRequest = paymentMapper.convertPaymentToAccUtilizationRequest(payment)
        setAccountUtilizationModel(accUtilizationModel, receivableRequest)

        val accUtilEntity = accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel)

        accUtilEntity.accCode = AresModelConstants.AR_ACCOUNT_CODE
        accUtilEntity.documentNo = payment.paymentNum!!
        accUtilEntity.documentValue = payment.paymentNumValue
        accUtilEntity.taxableAmount = BigDecimal.ZERO

        if (receivableRequest.accMode == AccMode.AP) {
            accUtilEntity.accCode = AresModelConstants.AP_ACCOUNT_CODE
        }

        val accUtilRes = accountUtilizationRepository.save(accUtilEntity)

//        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest)
//
//        try {
//            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
//        } catch (ex: Exception) {
//            logger().error(ex.stackTraceToString())
//        }
        return OnAccountApiCommonResponseMigration(paymentId = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true, accUtilId = accUtilRes.id!!)
    }

    private fun setAccountUtilizationModel(accUtilizationModel: AccUtilizationRequest, receivableRequest: com.cogoport.ares.api.migration.model.PaymentMigration) {
        accUtilizationModel.zoneCode = receivableRequest.zone
        accUtilizationModel.serviceType = receivableRequest.serviceType
        accUtilizationModel.accType = AccountType.REC
        accUtilizationModel.currencyPayment = BigDecimal.ZERO
        accUtilizationModel.ledgerPayment = BigDecimal.ZERO
        accUtilizationModel.ledgerAmount = receivableRequest.ledAmount
        accUtilizationModel.ledCurrency = receivableRequest.ledCurrency!!
        accUtilizationModel.currency = receivableRequest.currency!!
        accUtilizationModel.docStatus = DocumentStatus.PROFORMA
    }
}
