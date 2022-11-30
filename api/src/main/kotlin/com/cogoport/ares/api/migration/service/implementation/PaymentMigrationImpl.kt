package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.EntityCodeMapping
import com.cogoport.ares.api.migration.constants.MigrationConstants
import com.cogoport.ares.api.migration.constants.MigrationStatus
import com.cogoport.ares.api.migration.constants.SageBankMapping
import com.cogoport.ares.api.migration.constants.SettlementTypeMigration
import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.entity.MigrationLogsSettlements
import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import com.cogoport.ares.api.migration.model.GetOrgDetailsRequest
import com.cogoport.ares.api.migration.model.GetOrgDetailsResponse
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.OnAccountApiCommonResponseMigration
import com.cogoport.ares.api.migration.model.PaymentMigrationModel
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdsInput
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.repository.AccountUtilizationRepositoryMigration
import com.cogoport.ares.api.migration.repository.PaymentMigrationRepository
import com.cogoport.ares.api.migration.repository.SettlementsMigrationRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional

class PaymentMigrationImpl : PaymentMigration {

    @Inject lateinit var paymentMigrationRepository: PaymentMigrationRepository

    @Inject lateinit var migrationLogService: MigrationLogService

    @Inject lateinit var cogoClient: AuthClient

    @Inject lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject lateinit var accountUtilizationRepositoryMigration: AccountUtilizationRepositoryMigration

    @Inject lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject lateinit var settlementRepository: SettlementRepository

    @Inject lateinit var settlementMigrationRepository: SettlementsMigrationRepository
    override suspend fun migratePayment(paymentRecord: PaymentRecord): Int {
        var paymentRequest: PaymentMigrationModel? = null
        try {
            if (paymentMigrationRepository.checkPaymentExists(
                    paymentRecord.paymentNum!!,
                    AccMode.valueOf(paymentRecord.accMode!!).name,
                    PaymentCode.valueOf(paymentRecord.paymentCode!!).name,
                    AccountType.valueOf(paymentRecord.accountType!!).name
                )
            ) {
                throw AresException(AresError.ERR_1010, "Not migrating as payment already exists")
            }
            /*FETCH ORGANIZATION DETAILS BY SAGE ORGANIZATION ID*/
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = paymentRecord.sageOrganizationId,
                    organizationType = if (paymentRecord.accMode.equals("AR")) "income" else "expense"
                )
            )
            if (response == null || response.organizationId.isNullOrEmpty()) {
                val message = "Organization id is null, not migrating payment ${paymentRecord.paymentNum}"
                logger().info(message)
                migrationLogService.saveMigrationLogs(null, null, paymentRecord.paymentNum, null, null, null, null, null, null, message)
                return 0
            }
            paymentRequest = getPaymentRequest(paymentRecord, response)

            val paymentResponse: OnAccountApiCommonResponseMigration = createPaymentEntry(paymentRequest)

            migrationLogService.saveMigrationLogs(
                paymentResponse.paymentId, paymentResponse.accUtilId, paymentRequest.paymentNumValue, paymentRequest.currency, paymentRequest.amount,
                paymentRequest.ledAmount, paymentRequest.bankPayAmount, paymentRequest.accountUtilCurrAmount, paymentRequest.accountUtilLedAmount, null
            )
            logger().info("Payment with paymentId ${paymentRecord.paymentNum} was successfully migrated")
        } catch (ex: Exception) {
            var errorMessage = ex.stackTraceToString()
            if (errorMessage.length> 5000) {
                errorMessage = errorMessage.substring(0, 4998)
            }
            logger().error("Error while migrating payment with paymentId ${paymentRecord.paymentNum} " + ex.stackTraceToString())
            migrationLogService.saveMigrationLogs(null, null, errorMessage, paymentRecord.paymentNum)
        }
        return 1
    }

    override suspend fun migarteJournalVoucher(journalVoucherRecord: JournalVoucherRecord): Int {
        var paymentRequest: PaymentMigrationModel? = null
        try {
            if (paymentMigrationRepository.checkJVExists(
                    journalVoucherRecord.paymentNum!!,
                    journalVoucherRecord.accMode!!,
                    AccountType.valueOf(journalVoucherRecord.accountType!!).name
                )
            ) {
                throw AresException(AresError.ERR_1010, "JV record is already present")
            }
            /*FETCH ORGANIZATION DETAILS BY SAGE ORGANIZATION ID*/
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = journalVoucherRecord.sageOrganizationId,
                    organizationType = if (journalVoucherRecord.accMode.equals("AR")) "income" else "expense"
                )
            )
            if (response == null || response.organizationId.isNullOrEmpty()) {
                val message = "Organization id is null, not migrating journal voucher ${journalVoucherRecord.paymentNum}"
                logger().info(message)
                migrationLogService.saveMigrationLogs(
                    null, null, journalVoucherRecord.paymentNum, null, null,
                    null, null, null, null, message
                )
                return 0
            }

            val accUtilEntity = setAccountUtilizationsForJV(journalVoucherRecord, response)
            val accUtilRes = accountUtilizationRepositoryMigration.save(accUtilEntity)
            val jv = convertToJournalVoucherEntity(getJournalVoucherRequest(journalVoucherRecord, response), journalVoucherRecord)
            journalVoucherRepository.save(jv)
            try {
                Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
                emitDashboardAndOutstandingEvent(accUtilRes.dueDate!!, accUtilRes.transactionDate!!, accUtilRes.zoneCode, accUtilRes.accMode, accUtilRes.organizationId!!, accUtilRes.organizationName!!)
            } catch (ex: Exception) {
                logger().error(ex.stackTraceToString())
            }
            migrationLogService.saveMigrationLogs(
                null, accUtilRes.id, journalVoucherRecord.paymentNum, journalVoucherRecord.currency,
                journalVoucherRecord.accountUtilAmtLed, journalVoucherRecord.accountUtilAmtLed, null,
                journalVoucherRecord.accountUtilPayCurr, journalVoucherRecord.accountUtilPayLed, null
            )
            logger().info("Journal Voucher with ID ${journalVoucherRecord.paymentNum} was successfully migrated")
        } catch (ex: Exception) {
            var errorMessage = ex.stackTraceToString()
            if (errorMessage.length> 5000) {
                errorMessage = errorMessage.substring(0, 4998)
            }
            logger().error("Error while migrating journal voucher with ID ${journalVoucherRecord.paymentNum} " + ex.stackTraceToString())
            migrationLogService.saveMigrationLogs(null, null, errorMessage, journalVoucherRecord.paymentNum)
        }
        return 1
    }

    private fun getPaymentRequest(paymentRecord: PaymentRecord, rorOrgDetails: GetOrgDetailsResponse): PaymentMigrationModel {

        return PaymentMigrationModel(
            id = null,
            entityCode = paymentRecord.entityCode!!,
            orgSerialId = rorOrgDetails.organizationSerialId?.toLong(),
            sageOrganizationId = paymentRecord.sageOrganizationId!!,
            organizationId = UUID.fromString(rorOrgDetails.organizationId),
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
            transRefNumber = if (paymentRecord.narration.isNullOrEmpty()) null else getUTR(paymentRecord.narration!!),
            refPaymentId = null,
            transactionDate = paymentRecord.transactionDate,
            isPosted = true,
            isDeleted = false,
            createdAt = paymentRecord.createdAt!!,
            updatedAt = paymentRecord.updatedAt!!,
            cogoAccountNo = getCogoAccountNo(paymentRecord.bankShortCode!!),
            zone = rorOrgDetails.zone?.uppercase(),
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
            accountUtilPayLed = paymentRecord.accountUtilPayLed,
            bankPayAmount = paymentRecord.bankPayAmount,
            tradePartySerialId = rorOrgDetails.tradePartySerialId
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
        val accUtilRes = accountUtilizationRepositoryMigration.save(accUtilEntity)
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest)

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            emitDashboardAndOutstandingEvent(accUtilRes.dueDate!!, accUtilRes.transactionDate!!, accUtilRes.zoneCode, accUtilRes.accMode, accUtilRes.organizationId!!, accUtilRes.organizationName!!)
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }

        return OnAccountApiCommonResponseMigration(paymentId = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true, accUtilId = accUtilRes.id!!)
    }

    private suspend fun setPaymentEntry(receivableRequest: PaymentMigrationModel): PaymentMigrationEntity {
        // val tradePartyResponse = getTradePartyInfo(receivableRequest.organizationId.toString())
        val serialIdInputs = SerialIdsInput(receivableRequest.orgSerialId!!, receivableRequest.tradePartySerialId!!.toLong())

        val serialIdRequest = SerialIdDetailsRequest(
            organizationTradePartyMappings = arrayListOf(serialIdInputs)
        )
        val tradePartyResponse = cogoClient.getSerialIdDetails(serialIdRequest)

        return PaymentMigrationEntity(
            id = null,
            entityCode = receivableRequest.entityCode,
            orgSerialId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.tradePartySerial != null) tradePartyResponse.get(0)?.tradePartySerial else null,
            sageOrganizationId = receivableRequest.sageOrganizationId,
            organizationId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.organizationTradePartyDetailId != null) tradePartyResponse.get(0)?.organizationTradePartyDetailId else null,
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
            tradePartyMappingId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.mappingId != null) tradePartyResponse.get(0)?.mappingId else null,
            taggedOrganizationId = receivableRequest.organizationId,
            bankPayAmount = receivableRequest.bankPayAmount,
            migrated = true
        )
    }

    private fun setAccountUtilizations(receivableRequest: PaymentMigrationModel, paymentEntity: PaymentMigrationEntity): AccountUtilizationMigration {
        return AccountUtilizationMigration(
            id = null,
            documentNo = receivableRequest.paymentNum,
            documentValue = receivableRequest.paymentNumValue,
            zoneCode = receivableRequest.zone ?: "WEST",
            serviceType = ServiceType.NA.name,
            documentStatus = DocumentStatus.FINAL,
            entityCode = receivableRequest.entityCode,
            category = null,
            orgSerialId = paymentEntity.orgSerialId ?: 0L,
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
            taxableAmount = BigDecimal.ZERO,
            migrated = true
        )
    }

    private suspend fun setAccountUtilizationsForJV(receivableRequest: JournalVoucherRecord, orgDetailsResponse: GetOrgDetailsResponse): AccountUtilizationMigration {

        // val tradePartyResponse = getTradePartyInfo(orgDetailsResponse.organizationId.toString())
        val serialIdInputs = SerialIdsInput(orgDetailsResponse.organizationSerialId!!.toLong(), orgDetailsResponse.tradePartySerialId!!.toLong())

        val serialIdRequest = SerialIdDetailsRequest(
            organizationTradePartyMappings = arrayListOf(serialIdInputs)
        )
        val tradePartyResponse = cogoClient.getSerialIdDetails(serialIdRequest)

        return AccountUtilizationMigration(
            id = null,
            documentNo = getPaymentNum(receivableRequest.paymentNum),
            documentValue = receivableRequest.paymentNum,
            zoneCode = if (orgDetailsResponse.zone == null) "WEST" else orgDetailsResponse.zone.uppercase(),
            serviceType = ServiceType.NA.name,
            documentStatus = DocumentStatus.FINAL,
            entityCode = receivableRequest.entityCode!!,
            category = null,
            orgSerialId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.tradePartySerial != null) tradePartyResponse.get(0)?.tradePartySerial else 0,
            sageOrganizationId = receivableRequest.sageOrganizationId,
            organizationId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.organizationTradePartyDetailId != null) tradePartyResponse.get(0)?.organizationTradePartyDetailId else null,
            organizationName = receivableRequest.organizationName,
            accMode = AccMode.valueOf(receivableRequest.accMode!!),
            accCode = receivableRequest.accCode!!,
            accType = AccountType.valueOf(receivableRequest.accountType!!),
            signFlag = receivableRequest.signFlag!!,
            currency = receivableRequest.currency!!,
            ledCurrency = receivableRequest.ledgerCurrency!!,
            amountCurr = receivableRequest.accountUtilAmtCurr,
            amountLoc = receivableRequest.accountUtilAmtLed,
            payCurr = receivableRequest.accountUtilPayCurr,
            payLoc = receivableRequest.accountUtilPayLed,
            dueDate = receivableRequest.transactionDate,
            transactionDate = receivableRequest.transactionDate,
            createdAt = receivableRequest.createdAt,
            updatedAt = receivableRequest.updatedAt,
            tradePartyMappingId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.mappingId != null) tradePartyResponse.get(0)?.mappingId else null,
            taggedOrganizationId = UUID.fromString(orgDetailsResponse.organizationId),
            taxableAmount = BigDecimal.ZERO,
            migrated = true
        )
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * to update Dashboard and Receivables outstanding documents on OpenSearch
     * @param accUtilizationRequest
     */
    private fun emitDashboardAndOutstandingEvent(dueDate: Date, transactionDate: Date, zoneCode: String?, accMode: AccMode, organizationId: UUID, organizationName: String) {
        val date = dueDate ?: transactionDate
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(date),
                    quarter = date!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().get(IsoFields.QUARTER_OF_YEAR),
                    year = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year,
                    accMode = accMode
                )
            )
        )
        aresKafkaEmitter.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = zoneCode,
                    orgId = organizationId.toString(),
                    orgName = organizationName
                )
            )
        )
    }

    private suspend fun getJournalVoucherRequest(journalVoucherRecord: JournalVoucherRecord, response: GetOrgDetailsResponse): JournalVoucherRequest {
        val serialIdInputs = response.organizationSerialId?.let { response.tradePartySerialId?.let { it1 -> SerialIdsInput(it.toLong(), it1.toLong()) } }
        val serialIdRequest = SerialIdDetailsRequest(
            organizationTradePartyMappings = arrayListOf(serialIdInputs!!)
        )
        val tradePartyResponse = cogoClient.getSerialIdDetails(serialIdRequest)
        return JournalVoucherRequest(
            id = null,
            entityCode = journalVoucherRecord.entityCode!!,
            entityId = EntityCodeMapping.getByEntityCode(journalVoucherRecord.entityCode.toString()),
            jvNum = journalVoucherRecord.paymentNum,
            type = getTypeForJV(journalVoucherRecord.accMode!!, journalVoucherRecord.signFlag!!),
            status = JVStatus.APPROVED,
            category = JVCategory.JVNOS,
            validityDate = journalVoucherRecord.transactionDate!!,
            amount = journalVoucherRecord.accountUtilPayLed,
            currency = journalVoucherRecord.currency!!,
            ledCurrency = journalVoucherRecord.ledgerCurrency!!,
            exchangeRate = journalVoucherRecord.exchangeRate!!,
            tradePartyId = tradePartyResponse?.get(0)?.organizationTradePartyDetailId!!,
            tradePartyName = tradePartyResponse[0]?.tradePartyBusinessName!!,
            createdBy = MigrationConstants.createdUpdatedBy,
            accMode = AccMode.valueOf(journalVoucherRecord.accMode!!),
            description = null
        )
    }
    private fun getTypeForJV(accMode: String, signFlag: Short): String {
        if (accMode.equals("AR") && signFlag.compareTo(-1) == 0) {
            return "credit"
        } else if (accMode.equals("AR") && signFlag.compareTo(-1) == 0) {
            return "debit"
        } else if (accMode.equals("AP") && signFlag.compareTo(1) == 0) {
            return "debit"
        }
        return "credit"
    }

    private fun convertToJournalVoucherEntity(request: JournalVoucherRequest, journalVoucherRecord: JournalVoucherRecord): JournalVoucher {
        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.createdAt = journalVoucherRecord.createdAt
        jv.updatedAt = journalVoucherRecord.updatedAt
        return jv
    }

    override suspend fun migrateSettlements(settlementRecord: SettlementRecord) {
        try {
            if (settlementRecord.sourceType!! == "NOSTR") {
                return
            }
            val settlement = getSettlementEntity(settlementRecord)
            if (paymentMigrationRepository.checkDuplicateForSettlements(
                    settlement.sourceId!!,
                    settlement.destinationId,
                    settlement.ledAmount
                )
            ) {
                throw AresException(AresError.ERR_1010, "Settlement entry is already present")
            }

            settlementRepository.save(settlement)
            settlementMigrationRepository.save(
                MigrationLogsSettlements(
                    id = null,
                    sourceId = settlement.sourceId.toString(),
                    sourceValue = settlementRecord.paymentNumValue,
                    destinationId = settlement.destinationId.toString(),
                    destinationValue = settlementRecord.invoiceId,
                    ledgerCurrency = settlementRecord.ledger_currency,
                    ledgerAmount = settlementRecord.ledgerAmount,
                    accMode = settlementRecord.acc_mode,
                    status = MigrationStatus.MIGRATED.name,
                    errorMessage = null,
                    migrationDate = Timestamp(Date().time)
                )
            )
        } catch (ex: AresException) {
            logger().info("Error while migrating settlements ${settlementRecord.paymentNumValue}")
            settlementMigrationRepository.save(
                MigrationLogsSettlements(
                    id = null,
                    sourceId = null,
                    sourceValue = settlementRecord.paymentNumValue,
                    destinationId = null,
                    destinationValue = settlementRecord.invoiceId,
                    ledgerCurrency = settlementRecord.ledger_currency,
                    ledgerAmount = settlementRecord.ledgerAmount,
                    accMode = settlementRecord.acc_mode,
                    status = MigrationStatus.FAILED.name,
                    errorMessage = ex.context,
                    migrationDate = Timestamp(Date().time)
                )
            )
        }
    }

    private suspend fun getSettlementEntity(settlementRecord: SettlementRecord): Settlement {
        val sourceId: Long? = paymentMigrationRepository.getPaymentId(
            settlementRecord.paymentNumValue!!,
            settlementRecord.acc_mode!!,
            settlementRecord.sourceType!!,
            settlementRecord.sageOrganizationId!!
        )

        var destinationId: Long? = paymentMigrationRepository.getDestinationId(
            settlementRecord.invoiceId!!,
            settlementRecord.acc_mode!!,
            settlementRecord.sageOrganizationId!!
        )

        if (destinationId == null) {
            destinationId = paymentMigrationRepository.getDestinationIdForAr(
                settlementRecord.invoiceId!!,
                settlementRecord.acc_mode!!
            )
        }

        if (sourceId == null || destinationId == null) {
            throw AresException(AresError.ERR_1002, "Cannot migrate as sourceId or DestinationId is null")
        }
        return Settlement(
            id = null,
            sourceId = sourceId,
            sourceType = SettlementTypeMigration.getSettlementType(settlementRecord.sourceType!!),
            destinationId = destinationId,
            destinationType = SettlementTypeMigration.getSettlementType(settlementRecord.destinationType!!),
            currency = settlementRecord.currency,
            amount = settlementRecord.currencyAmount,
            ledCurrency = settlementRecord.ledger_currency!!,
            ledAmount = settlementRecord.ledgerAmount!!,
            signFlag = getSignFlag(settlementRecord.sourceType!!),
            settlementDate = settlementRecord.createdAt!!,
            createdBy = MigrationConstants.createdUpdatedBy,
            createdAt = settlementRecord.createdAt,
            updatedBy = MigrationConstants.createdUpdatedBy,
            updatedAt = settlementRecord.updatedAt
        )
    }
    private fun getSignFlag(sourceType: String): Short {
        if (sourceType.equals(SettlementType.NOSTRO.name)) {
            return -1
        }
        return 1
    }
}
