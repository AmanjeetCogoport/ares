package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.MigrationConstants
import com.cogoport.ares.api.migration.constants.PaymentModeMapping
import com.cogoport.ares.api.migration.mapper.PaymentMapper
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.repository.PaymentMigrationRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.*
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import java.math.BigDecimal
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.transaction.Transactional

class PaymentMigrationImpl : PaymentMigration {

    @Inject lateinit var paymentMapper: PaymentMapper

    @Inject lateinit var paymentMigrationRepository: PaymentMigrationRepository

    @Inject lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    @Inject lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject lateinit var migrationLogService: MigrationLogService

    override suspend fun migratePayment(paymentRecord: PaymentRecord) {
        try {
            val paymentRequest = getPaymentRequest(paymentRecord)
            // get  data from ROR and map it to the model class
            createPaymentEntry(paymentRequest)
            migrationLogService.saveMigrationLogs(paymentRecord.paymentNumValue)
            logger().info("Payment with paymentId ${paymentRecord.paymentNumValue} was successfully migrated")
        } catch (ex: Exception) {
            migrationLogService.saveMigrationLogs(paymentRecord.paymentNumValue, ex)
            logger().error("Error while migrating payment with paymentId ${paymentRecord.paymentNumValue}")
            logger().info("******* Printing Stack trace ******** ${ex.stackTraceToString()}")
        }
    }

    private fun getPaymentRequest(paymentRecord: PaymentRecord): Payment {
        return Payment(
            id = null,
            entityType = paymentRecord.entityCode,
            fileId = null,
            orgSerialId = null,
            sageOrganizationId = null,
            organizationId = paymentRecord.organizationId,
            organizationName = paymentRecord.organizationName,
            accCode = paymentRecord.accCode,
            accMode = AccMode.AR,
            signFlag = paymentRecord.signFlag,
            currency = paymentRecord.currency,
            amount = paymentRecord.amount,
            ledCurrency = paymentRecord.ledCurrency,
            ledAmount = paymentRecord.ledAmount,
            payMode = PaymentModeMapping.getPayMode(paymentRecord.paymentMode!!),
            remarks = paymentRecord.narration,
            utr = null,
            refPaymentId = null,
            transactionDate = paymentRecord.transactionDate,
            isPosted = paymentRecord.isPosted,
            isDeleted = paymentRecord.isDeleted,
            createdAt = paymentRecord.createdAt,
            createdBy = MigrationConstants.createdUpdatedBy.toString(),
            updatedAt = paymentRecord.updatedAt,
            bankAccountNumber = null,
            zone = null,
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

    private fun getPaymentNum(paymentNum: String?): Long {
        val matcher: Matcher = Pattern.compile("\\d+").matcher(paymentNum)
        matcher.find()
        return Integer.valueOf(matcher.group()).toLong()
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
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

        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest)

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }
        return OnAccountApiCommonResponse(id = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    private fun setAccountUtilizationModel(accUtilizationModel: AccUtilizationRequest, receivableRequest: Payment) {
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
