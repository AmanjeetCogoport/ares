package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.events.PlutusMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.AccountTypeMapping
import com.cogoport.ares.api.migration.constants.EntityCodeMapping
import com.cogoport.ares.api.migration.constants.MigrationConstants
import com.cogoport.ares.api.migration.constants.MigrationConstants.inactiveBRPNo
import com.cogoport.ares.api.migration.constants.MigrationRecordType
import com.cogoport.ares.api.migration.constants.MigrationStatus
import com.cogoport.ares.api.migration.constants.SageBankMapping
import com.cogoport.ares.api.migration.constants.SettlementTypeMigration
import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.entity.JournalVoucherMigration
import com.cogoport.ares.api.migration.entity.JvResponse
import com.cogoport.ares.api.migration.entity.MigrationLogsSettlements
import com.cogoport.ares.api.migration.entity.ParentJournalVoucherMigration
import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import com.cogoport.ares.api.migration.model.GetOrgDetailsRequest
import com.cogoport.ares.api.migration.model.GetOrgDetailsResponse
import com.cogoport.ares.api.migration.model.JVLineItemNoBPR
import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.OnAccountApiCommonResponseMigration
import com.cogoport.ares.api.migration.model.PaidUnpaidStatus
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentMigrationModel
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdsInput
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.repository.AccountUtilizationRepositoryMigration
import com.cogoport.ares.api.migration.repository.JournalVoucherRepoMigration
import com.cogoport.ares.api.migration.repository.ParentJVRepoMigration
import com.cogoport.ares.api.migration.repository.PaymentMigrationRepository
import com.cogoport.ares.api.migration.repository.SettlementsMigrationRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
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
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.brahma.opensearch.Client
import com.cogoport.kuber.client.KuberClient
import io.sentry.Sentry
import jakarta.inject.Inject
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional

class PaymentMigrationImpl : PaymentMigration {

    @Inject lateinit var paymentMigrationRepository: PaymentMigrationRepository

    @Inject lateinit var migrationLogService: MigrationLogService

    @Inject lateinit var paymentNumGeneratorRepo: PaymentNumGeneratorRepo

    @Inject lateinit var cogoClient: AuthClient

    @Inject lateinit var accountUtilizationRepositoryMigration: AccountUtilizationRepositoryMigration

    @Inject lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject lateinit var settlementRepository: SettlementRepository

    @Inject lateinit var settlementMigrationRepository: SettlementsMigrationRepository

    @Inject lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject lateinit var kuberMessagePublisher: KuberMessagePublisher

    @Inject lateinit var plutusMessagePublisher: PlutusMessagePublisher

    @Inject lateinit var sageServiceImpl: SageServiceImpl

    @Inject lateinit var parentJournalVoucherRepo: ParentJVRepoMigration

    @Inject lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject lateinit var journalVoucherRepoMigration: JournalVoucherRepoMigration

    @Inject lateinit var kuberClient: KuberClient

    override suspend fun migratePayment(paymentRecord: PaymentRecord): Int {
        var paymentRequest: PaymentMigrationModel? = null
        try {
            if (paymentMigrationRepository.checkPaymentExists(
                    paymentRecord.sageRefNumber!!,
                    AccMode.valueOf(paymentRecord.accMode!!).name,
                    PaymentCode.valueOf(paymentRecord.paymentCode!!).name,
                    AccountType.valueOf(paymentRecord.accountType!!).name
                )
            ) {
                throw AresException(AresError.ERR_1010, "Not migrating as payment already exists")
            }

            paymentRecord.sageOrganizationId = if (inactiveBRPNo.containsKey(paymentRecord.sageOrganizationId)) inactiveBRPNo[paymentRecord.sageOrganizationId] else paymentRecord.sageOrganizationId
            /*FETCH ORGANIZATION DETAILS BY SAGE ORGANIZATION ID*/
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = paymentRecord.sageOrganizationId,
                    organizationType = if (paymentRecord.accMode == "AR") "income" else "expense",
                    cogoEntityId = AresConstants.ENTITY_ID[paymentRecord.entityCode]
                )
            )
            if (response == null || response.organizationId.isNullOrEmpty()) {
                val message = "Organization id is null, not migrating payment ${paymentRecord.sageRefNumber}"
                logger().info(message)
                migrationLogService.saveMigrationLogs(null, null, paymentRecord.sageRefNumber, null, null, null, null, null, null, message)
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
            if (errorMessage.length > 5000) {
                errorMessage = errorMessage.substring(0, 4998)
            }
            logger().error("Error while migrating payment with paymentId ${paymentRecord.paymentNum} " + ex.stackTraceToString())
            migrationLogService.saveMigrationLogs(null, null, errorMessage, paymentRecord.sageRefNumber, MigrationStatus.FAILED)
        }
        return 1
    }

    override suspend fun migrateJournalVoucher(journalVoucherRecord: JournalVoucherRecord, parentJvId: Long) {
        var paymentRequest: PaymentMigrationModel? = null
        try {
            val jvId = paymentMigrationRepository.checkJVWithNoBpr(
                journalVoucherRecord.sageUniqueId!!,
                journalVoucherRecord.paymentNum!!
            )
            var jvResponse: JvResponse? = null
            if (jvId != null) {
                jvResponse = paymentMigrationRepository.checkJVExists(
                    journalVoucherRecord.paymentNum,
                    jvId
                )
            }
            if (jvResponse != null) {
                if (jvResponse.updatedAt < journalVoucherRecord.updatedAt) {
                    journalVoucherRepository.deleteById(jvResponse.jvId)
                    accountUtilizationRepositoryMigration.deleteById(jvResponse.accountUtilizationId)
                } else {
                    return
                }
            }

            /*FETCH ORGANIZATION DETAILS BY SAGE ORGANIZATION ID*/
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = journalVoucherRecord.sageOrganizationId,
                    organizationType = if (journalVoucherRecord.accMode.equals("AR")) "income" else "expense",
                    cogoEntityId = AresConstants.ENTITY_ID[journalVoucherRecord.entityCode]
                )
            )
            if (response == null || response.organizationId.isNullOrEmpty()) {
                val message = "Organization id is null, not migrating journal voucher ${journalVoucherRecord.paymentNum} BPR - ${journalVoucherRecord.sageOrganizationId}"
                logger().info(message)
                migrationLogService.saveMigrationLogs(
                    null, null, journalVoucherRecord.paymentNum, null, null,
                    null, null, null, null, message
                )
                return
            }
            val jv = convertToJournalVoucherEntity(getJournalVoucherRequest(journalVoucherRecord, response), journalVoucherRecord, parentJvId)
            val jvRecord = journalVoucherRepoMigration.save(jv)
            val accUtilEntity = setAccountUtilizationsForJV(journalVoucherRecord, response, jvRecord.id!!)
            val accUtilRes = accountUtilizationRepositoryMigration.save(accUtilEntity)
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
            var errorMessage = "BPR: ${journalVoucherRecord.sageOrganizationId} ::"
            errorMessage += ex.stackTraceToString()
            if (errorMessage.length > 5000) {
                errorMessage = errorMessage.substring(0, 4500)
            }
            logger().error("Error while migrating journal voucher with ID ${journalVoucherRecord.paymentNum} " + ex.stackTraceToString())
            migrationLogService.saveMigrationLogs(null, null, errorMessage, journalVoucherRecord.paymentNum, MigrationStatus.FAILED)
        }
    }

    private suspend fun getPaymentRequest(paymentRecord: PaymentRecord, rorOrgDetails: GetOrgDetailsResponse): PaymentMigrationModel {

        val paymentSeq = setPaymentEntity(paymentRecord)
        return PaymentMigrationModel(
            id = null,
            entityCode = paymentRecord.entityCode!!,
            orgSerialId = null, // rorOrgDetails.organizationSerialId?.toLong(),
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
            transRefNumber = paymentRecord.narration,
            refPaymentId = null,
            transactionDate = paymentRecord.transactionDate,
            createdAt = paymentRecord.createdAt!!,
            updatedAt = paymentRecord.updatedAt!!,
            cogoAccountNo = getCogoAccountNo(paymentRecord.bankShortCode!!),
            zone = rorOrgDetails.zone?.uppercase(),
            serviceType = ServiceType.NA,
            paymentCode = PaymentCode.valueOf(paymentRecord.paymentCode!!),
            bankName = getCogoBankName(paymentRecord.bankShortCode) ?: paymentRecord.bankShortCode,
            exchangeRate = paymentRecord.exchangeRate!!,
            paymentNum = paymentSeq.paymentNum!!,
            paymentNumValue = paymentSeq.paymentNumValue!!,
            bankId = getCogoBankId(paymentRecord.bankShortCode),
            accountType = AccountType.valueOf(paymentRecord.accountType!!),
            accountUtilCurrAmount = paymentRecord.accountUtilAmtCurr,
            accountUtilLedAmount = paymentRecord.accountUtilAmtLed,
            accountUtilPayCurr = paymentRecord.accountUtilPayCurr,
            accountUtilPayLed = paymentRecord.accountUtilPayLed,
            bankPayAmount = paymentRecord.bankPayAmount,
            tradePartySerialId = rorOrgDetails.tradePartySerialId,
            sageRefNumber = paymentRecord.sageRefNumber
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

    private suspend fun setPaymentEntity(paymentRecord: PaymentRecord): PaymentRecord {
        if (AccMode.valueOf(paymentRecord.accMode!!) == AccMode.AR) {
            paymentRecord.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
            paymentRecord.paymentNumValue = SequenceSuffix.RECEIVED.prefix + paymentRecord.paymentNum
        } else {
            paymentRecord.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.PAYMENT.prefix)
            paymentRecord.paymentNumValue = SequenceSuffix.PAYMENT.prefix + paymentRecord.paymentNum
        }

        return paymentRecord
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    suspend fun createPaymentEntry(receivableRequest: PaymentMigrationModel): OnAccountApiCommonResponseMigration {
        /*SAVE PAYMENTS*/
        val payment = setPaymentEntry(receivableRequest)
        val savedPayment = paymentMigrationRepository.save(payment)
        receivableRequest.id = savedPayment.id

        val accUtilEntity = setAccountUtilizations(receivableRequest, payment)
        val accUtilRes = accountUtilizationRepositoryMigration.save(accUtilEntity)
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
        val organizationSerialId = cogoClient.getCogoOrganization(
            CogoOrganizationRequest(
                organizationSerialId = null,
                organizationId = receivableRequest.organizationId!!.toString()
            )
        ).organizationSerialId ?: throw AresException(AresError.ERR_1008, "organization serial_id not found")

        val serialIdInputs = SerialIdsInput(organizationSerialId, receivableRequest.tradePartySerialId!!.toLong(), AresConstants.ENTITY_ID[receivableRequest.entityCode])

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
            migrated = true,
            paymentDocumentStatus = PaymentDocumentStatus.FINAL_POSTED,
            updatedBy = MigrationConstants.createdUpdatedBy,
            createdBy = MigrationConstants.createdUpdatedBy,
            sageRefNumber = receivableRequest.sageRefNumber
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
            migrated = true,
            settlementEnabled = true
        )
    }

    private suspend fun setAccountUtilizationsForJV(receivableRequest: JournalVoucherRecord, orgDetailsResponse: GetOrgDetailsResponse, jvId: Long): AccountUtilizationMigration {
        // need to call ROR API for org_serial_id
        val organizationSerialId = cogoClient.getCogoOrganization(
            CogoOrganizationRequest(
                organizationSerialId = null,
                organizationId = orgDetailsResponse.organizationId
            )
        ).organizationSerialId ?: throw AresException(AresError.ERR_1008, "organization serial_id not found")

        // val tradePartyResponse = getTradePartyInfo(orgDetailsResponse.organizationId.toString())
        val serialIdInputs = SerialIdsInput(organizationSerialId!!, orgDetailsResponse.tradePartySerialId!!.toLong(), AresConstants.ENTITY_ID[receivableRequest.entityCode])

        val serialIdRequest = SerialIdDetailsRequest(
            organizationTradePartyMappings = arrayListOf(serialIdInputs)
        )
        val tradePartyResponse = cogoClient.getSerialIdDetails(serialIdRequest)

        return AccountUtilizationMigration(
            id = null,
            documentNo = jvId,
            documentValue = receivableRequest.paymentNum,
            zoneCode = if (orgDetailsResponse.zone == null) "WEST" else orgDetailsResponse.zone.uppercase(),
            serviceType = ServiceType.NA.name,
            documentStatus = DocumentStatus.FINAL,
            entityCode = receivableRequest.entityCode!!,
            category = null,
            orgSerialId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.tradePartySerial != null) tradePartyResponse.get(0)?.tradePartySerial else 0,
            sageOrganizationId = receivableRequest.sageOrganizationId,
            organizationId = if (tradePartyResponse != null && tradePartyResponse.get(0)?.organizationTradePartyDetailId != null) tradePartyResponse.get(0)?.organizationTradePartyDetailId else null,
            organizationName = if (tradePartyResponse?.get(0)?.tradePartyBusinessName.isNullOrEmpty()) receivableRequest.organizationName else tradePartyResponse?.get(0)?.tradePartyBusinessName,
            accMode = AccMode.valueOf(receivableRequest.accMode!!),
            accCode = receivableRequest.accCode!!,
            accType = AccountType.valueOf(AccountTypeMapping.getAccountType(receivableRequest.accountType!!)),
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
            migrated = true,
            taggedBillId = null,
            settlementEnabled = true
        )
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * to update Dashboard and Receivables outstanding documents on OpenSearch
     * @param accUtilizationRequest
     */
    private suspend fun emitDashboardAndOutstandingEvent(dueDate: Date, transactionDate: Date, zoneCode: String?, accMode: AccMode, organizationId: UUID, organizationName: String) {
        val date = dueDate ?: transactionDate
        aresMessagePublisher.emitOutstandingData(
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
        val organizationSerialId = cogoClient.getCogoOrganization(
            CogoOrganizationRequest(
                organizationSerialId = null,
                organizationId = response.organizationId
            )
        ).organizationSerialId ?: throw AresException(AresError.ERR_1008, "organization serial_id not found")

        val serialIdInputs = organizationSerialId.let { response.tradePartySerialId?.let { it1 -> SerialIdsInput(it, it1.toLong(), AresConstants.ENTITY_ID[journalVoucherRecord.entityCode]) } }
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
            status = JVStatus.POSTED,
            category = AccountTypeMapping.getAccountType(journalVoucherRecord.accountType!!),
            validityDate = journalVoucherRecord.transactionDate!!,
            amount = journalVoucherRecord.accountUtilAmtCurr,
            currency = journalVoucherRecord.currency!!,
            ledCurrency = journalVoucherRecord.ledgerCurrency!!,
            exchangeRate = journalVoucherRecord.exchangeRate!!,
            tradePartyId = tradePartyResponse?.get(0)?.organizationTradePartyDetailId!!,
            tradePartyName = tradePartyResponse[0]?.tradePartyBusinessName!!,
            createdBy = MigrationConstants.createdUpdatedBy,
            accMode = AccMode.valueOf(journalVoucherRecord.accMode),
            description = journalVoucherRecord.narration,
            glCode = journalVoucherRecord.accCode.toString(),
            signFlag = journalVoucherRecord.signFlag.toShort()
        )
    }

    private fun getTypeForJV(accMode: String, signFlag: Short): String {
        if (accMode.equals("AR") && signFlag.compareTo(-1) == 0) {
            return "credit"
        } else if (accMode.equals("AR") && signFlag.compareTo(1) == 0) {
            return "debit"
        } else if (accMode.equals("AP") && signFlag.compareTo(1) == 0) {
            return "debit"
        }
        return "credit"
    }

    private fun convertToJournalVoucherEntity(request: JournalVoucherRequest, journalVoucherRecord: JournalVoucherRecord, parentJvId: Long): JournalVoucherMigration {
        val jv = journalVoucherConverter.convertRequestToEntityMigration(request)
        jv.createdAt = journalVoucherRecord.createdAt
        jv.updatedAt = journalVoucherRecord.updatedAt
        jv.sageUniqueId = journalVoucherRecord.sageUniqueId
        jv.ledAmount = journalVoucherRecord.accountUtilAmtLed
        jv.migrated = true
        jv.parentJvId = parentJvId
        jv.glCode = request.glCode
        jv.signFlag = request.signFlag
        return jv
    }

    override suspend fun migrateSettlements(settlementRecord: SettlementRecord) {
        try {
            if (settlementRecord.sourceType!! == "NOSTR") {
                return
            }
            val settlement = getSettlementEntity(settlementRecord)
            settlement.settlementNum = sequenceGeneratorImpl.getSettlementNumber()
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
                    accMode = settlementRecord.accMode,
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
                    accMode = settlementRecord.accMode,
                    status = MigrationStatus.FAILED.name,
                    errorMessage = ex.context,
                    migrationDate = Timestamp(Date().time)
                )
            )
        }
    }

    private suspend fun getSettlementEntity(settlementRecord: SettlementRecord): Settlement {
        var sourceId: Long? = paymentMigrationRepository.getPaymentId(
            settlementRecord.paymentNumValue!!,
            settlementRecord.accMode!!,
            settlementRecord.sourceType!!,
            settlementRecord.sageOrganizationId!!
        )

        if (sourceId == null) {
            sourceId = paymentMigrationRepository.getPaymentIdWithoutPayCode(
                settlementRecord.paymentNumValue!!,
                settlementRecord.accMode!!,
                settlementRecord.accCode!!,
                settlementRecord.sageOrganizationId!!
            )
        }

        var destinationId: Long? = paymentMigrationRepository.getDestinationId(
            settlementRecord.invoiceId!!,
            settlementRecord.accMode!!,
            settlementRecord.sageOrganizationId!!
        )

        if (destinationId == null) {
            destinationId = paymentMigrationRepository.getDestinationIdForAr(
                settlementRecord.invoiceId!!,
                settlementRecord.accMode!!
            )
        }

        if (sourceId == null) {
            throw AresException(AresError.ERR_1002, "Cannot migrate as sourceId is null")
        }
        if (destinationId == null) {
            throw AresException(AresError.ERR_1002, "Cannot migrate as destinationId is null")
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
            updatedAt = settlementRecord.updatedAt,
            settlementNum = null,
            settlementStatus = SettlementStatus.POSTED
        )
    }
    private fun getSignFlag(sourceType: String): Short {
        if (sourceType.equals(SettlementType.NOSTRO.name)) {
            return -1
        }
        return 1
    }

    override suspend fun updatePayment(payLocUpdateRequest: PayLocUpdateRequest) {
        try {
            val organizationType = if (payLocUpdateRequest.accMode == "AP") "expense" else "income"
            val tradePartyDetailId = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = payLocUpdateRequest.sageOrganizationId,
                    organizationType = organizationType,
                    cogoEntityId = AresConstants.ENTITY_ID[payLocUpdateRequest.entityCode]
                )
            ).organizationTradePartyDetailId ?: throw AresException(AresError.ERR_1003, "organizationTradePartyDetailId not found")
            var migrationStatus = MigrationStatus.PAYLOC_UPDATED
            if (MigrationRecordType.PAYMENT == payLocUpdateRequest.recordType) {
                val documentValue = accountUtilizationRepositoryMigration.getPaymentDetails(
                    sageRefNumber = payLocUpdateRequest.documentValue!!,
                    accMode = payLocUpdateRequest.accMode!!,
                    organizationId = tradePartyDetailId
                )
                if (null != documentValue) payLocUpdateRequest.documentValue = documentValue
            }

            if (payLocUpdateRequest.recordType == MigrationRecordType.BILL) {
                val billDetails = kuberClient.getBillNumberFromSageNumber(payLocUpdateRequest.documentValue!!)
                payLocUpdateRequest.documentValue = billDetails.billNumber
            }

            val platformUtilizedPayment = accountUtilizationRepositoryMigration.getRecordFromAccountUtilization(
                payLocUpdateRequest.documentValue!!, payLocUpdateRequest.accMode!!, tradePartyDetailId
            ) ?: return
            if (platformUtilizedPayment.toBigInteger() == payLocUpdateRequest.payLoc?.toBigInteger()) {
                return
            }
//            if (platformUtilizedPayment.toBigInteger().compareTo(payLocUpdateRequest.payLoc?.toBigInteger()) == 1) {
//                migrationStatus = MigrationStatus.PAYLOC_EXCEEDS
//            }
            else {
                accountUtilizationRepositoryMigration
                    .updateUtilizationAmount(
                        payLocUpdateRequest.documentValue!!,
                        payLocUpdateRequest.payLoc!!,
                        payLocUpdateRequest.payCurr!!,
                        payLocUpdateRequest.accMode!!,
                        tradePartyDetailId
                    )
                val response = accountUtilizationRepositoryMigration.getAccType(
                    payLocUpdateRequest.documentValue!!,
                    payLocUpdateRequest.accMode,
                    tradePartyDetailId
                )
                var status = if (payLocUpdateRequest.payLoc.compareTo(BigDecimal.ZERO) == 0) {
                    "UNPAID"
                } else if (payLocUpdateRequest.amtLoc!! > payLocUpdateRequest.payLoc) {
                    "PARTIAL_PAID"
                } else {
                    "PAID"
                }
                if (AccountType.SINV.name == response.accType ||
                    AccountType.SCN.equals(response.accType)
                ) {
                    plutusMessagePublisher.emitInvoiceStatus(
                        PaidUnpaidStatus(
                            documentValue = payLocUpdateRequest.documentValue!!,
                            documentNumber = response.documentNo!!,
                            status = status
                        )
                    )
                }

                if (AccountType.PCN.name == response.accType ||
                    AccountType.PINV.name == response.accType
                ) {
                    if (status == "PARTIAL_PAID") {
                        status = "PARTIAL"
                    } else if (status == "PAID") {
                        status = "FULL"
                    }
                    kuberMessagePublisher.emitBIllStatus(
                        PaidUnpaidStatus(
                            documentValue = payLocUpdateRequest.documentValue!!,
                            documentNumber = response.documentNo!!,
                            status = status
                        )
                    )
                }
            }

            migrationLogService.saveMigrationLogs(null, null, null, payLocUpdateRequest.documentValue!!, migrationStatus)
        } catch (ex: Exception) {
            var errorMessage = ex.stackTraceToString()
            if (errorMessage.length > 5000) {
                errorMessage = errorMessage.substring(0, 4998)
            }
            logger().error("Error while updating utilized amount ${payLocUpdateRequest.documentValue} " + ex.stackTraceToString())
            migrationLogService.saveMigrationLogs(
                null, null, errorMessage,
                payLocUpdateRequest.documentValue, MigrationStatus.PAYLOC_NOT_UPDATED
            )
        }
    }

    override suspend fun migrateJV(jvParentDetail: JVParentDetails) {
        var jvParentRecord: ParentJournalVoucherMigration? = null
        var jvRecords: List<JournalVoucherRecord>? = null
        var parentJVId = parentJournalVoucherRepo.checkIfParentJVExists(jvParentDetail.jvNum, jvParentDetail.jvType)
        val jvRecordsWithoutBpr = sageServiceImpl.getJVLineItemWithNoBPR(jvParentDetail.jvNum, jvParentDetail.jvType)
        try {
            jvRecords = sageServiceImpl.getJournalVoucherFromSageCorrected(null, null, "'${jvParentDetail.jvNum}'", jvParentDetail.jvType)
            var sum = BigDecimal.ZERO
            jvRecords.forEach {
                sum += (it.accountUtilAmtLed * BigDecimal.valueOf(it.signFlag!!.toLong()))
            }
            jvRecordsWithoutBpr.forEach {
                sum += (it.ledgerAmount * it.signFlag)
            }
            if (sum.toBigInteger() != BigDecimal.ZERO.toBigInteger()) {
                migrationLogService.saveMigrationLogs(
                    null, null, jvParentDetail.jvNum, null, null,
                    null, null, null, null, "jv Sum is not zero"
                )
                return
            }
            if (parentJVId == null) {
                jvParentRecord = parentJournalVoucherRepo.save(
                    ParentJournalVoucherMigration(
                        id = null,
                        status = JVStatus.valueOf(jvParentDetail.jvStatus),
                        category = AccountTypeMapping.getAccountType(jvParentDetail.jvType),
                        jvNum = jvParentDetail.jvNum,
                        validityDate = jvParentDetail.validityDate,
                        createdAt = jvParentDetail.createdAt,
                        updatedAt = jvParentDetail.updatedAt,
                        createdBy = MigrationConstants.createdUpdatedBy,
                        updatedBy = MigrationConstants.createdUpdatedBy,
                        migrated = true,
                        currency = jvParentDetail.currency,
                        ledCurrency = jvParentDetail.ledgerCurrency,
                        exchangeRate = jvParentDetail.exchangeRate,
                        description = jvParentDetail.description,
                        jvCodeNum = jvParentDetail.jvCodeNum,
                        entityCode = jvRecords.firstOrNull()?.entityCode,
                        transactionDate = jvParentDetail.validityDate
                    )
                )
                parentJVId = jvParentRecord.id!!
                migrationLogService.saveMigrationLogs(
                    null, null, jvParentDetail.jvNum, jvParentDetail.currency,
                    jvParentDetail.amount, null, null,
                    null, null, null
                )
            }
        } catch (ex: Exception) {
            logger().error(ex.message)
            migrationLogService.saveMigrationLogs(
                null, null, jvParentDetail.jvNum, null, null,
                null, null, null, null, "Error while storing jv header: ${ex.message}"
            )
            return
        }
        storeJVLineItems(jvRecordsWithoutBpr, parentJVId)
        jvRecords.forEach {
            this.migrateJournalVoucher(it, parentJVId)
        }
    }

    private suspend fun storeJVLineItems(jvRecordsWithoutBpr: List<JVLineItemNoBPR>, parentJvId: Long) {
        jvRecordsWithoutBpr.forEach {
            try {
                val jvId = paymentMigrationRepository.checkJVWithNoBpr(it.sageUniqueId, it.jvNum)
                if (jvId != null) {
                    journalVoucherRepository.deleteById(jvId)
                }
                journalVoucherRepoMigration.save(
                    JournalVoucherMigration(
                        id = null,
                        entityId = EntityCodeMapping.getByEntityCode(it.entityCode!!),
                        entityCode = it.entityCode.toInt(),
                        jvNum = it.jvNum,
                        type = "",
                        category = AccountTypeMapping.getAccountType(it.type),
                        validityDate = it.validityDate,
                        amount = it.amount,
                        currency = it.currency,
                        ledCurrency = it.ledgerCurrency,
                        status = JVStatus.valueOf(it.status),
                        exchangeRate = it.exchangeRate,
                        tradePartyId = null,
                        tradePartyName = "",
                        createdBy = MigrationConstants.createdUpdatedBy,
                        createdAt = it.createdAt,
                        updatedBy = MigrationConstants.createdUpdatedBy,
                        updatedAt = it.updatedAt,
                        description = it.description,
                        accMode = if (it.accMode?.trim()?.length != 0) AccMode.valueOf(it.accMode!!) else AccMode.OTHER,
                        parentJvId = parentJvId,
                        sageUniqueId = it.sageUniqueId,
                        migrated = true,
                        glCode = it.glcode,
                        ledAmount = it.ledgerAmount,
                        signFlag = it.signFlag.toShort()
                    )
                )
                migrationLogService.saveMigrationLogs(
                    null, null, it.sageUniqueId, it.currency,
                    it.amount, it.ledgerAmount, null,
                    null, null, null
                )
            } catch (ex: Exception) {
                logger().error(ex.message)
                migrationLogService.saveMigrationLogs(
                    null, null, it.sageUniqueId, null, null,
                    null, null, null, null, "Error while stroing line items: ${it.sageUniqueId}"
                )
            }
        }
    }

    override suspend fun migrateSettlementNum(id: Long) {
        val prefix = SequenceSuffix.SETTLEMENT.prefix
        val number = paymentNumGeneratorRepo.getNextSequenceNumber(prefix)
        val settlementNum = "${prefix}222300000000$number"
        settlementRepository.updateSettlementNumber(id, settlementNum)
    }

    override suspend fun migrateNewPeriodRecords(record: NewPeriodRecord) {
        try {
            val response = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = record.sageOrganizationId,
                    organizationType = if (record.accMode.equals("AR")) "income" else "expense"
                )
            )
            val cogoOrganization = cogoClient.getCogoOrganization(
                CogoOrganizationRequest(
                    organizationSerialId = null,
                    organizationId = response.organizationId
                )
            )
            if (cogoOrganization.organizationSerialId == null) {
                throw AresException(AresError.ERR_1008, "Organization serial_id information not found")
            }
            val organizationSerialId = cogoOrganization.organizationSerialId!!
            val serialIdInputs = SerialIdsInput(organizationSerialId, response.tradePartySerialId!!.toLong(), AresConstants.ENTITY_ID[record.cogoEntity!!.toInt()])
            val serialIdRequest = SerialIdDetailsRequest(
                organizationTradePartyMappings = arrayListOf(serialIdInputs)
            )
            val tradePartyResponse = cogoClient.getSerialIdDetails(serialIdRequest)?.get(0)
                ?: throw AresException(AresError.ERR_1008, "trade party information not found")

            if (accountUtilizationRepositoryMigration.checkIfNewRecordIsPresent(record.documentValue!!, record.sageOrganizationId!!)) {
                throw AresException(AresError.ERR_1008, "new period record already present")
            }
            accountUtilizationRepositoryMigration.save(
                AccountUtilizationMigration(
                    id = null,
                    documentNo = getFormattedNumber(record.documentValue).toLong(),
                    documentValue = record.documentValue,
                    zoneCode = "NORTH",
                    serviceType = "NA",
                    documentStatus = DocumentStatus.FINAL,
                    entityCode = record.cogoEntity!!.toInt(),
                    category = null,
                    orgSerialId = tradePartyResponse.tradePartySerial,
                    sageOrganizationId = record.sageOrganizationId,
                    organizationId = tradePartyResponse.organizationTradePartyDetailId,
                    taggedOrganizationId = tradePartyResponse.organizationId,
                    tradePartyMappingId = tradePartyResponse.mappingId,
                    organizationName = tradePartyResponse.tradePartyBusinessName,
                    accCode = if (record.accMode == "AR") 223000 else 321000,
                    accType = AccountType.NEWPR,
                    accMode = if (record.accMode == "AR") AccMode.AR else AccMode.AP,
                    signFlag = record.signFlag!!.toShort(),
                    currency = record.currency!!,
                    ledCurrency = record.currency,
                    amountCurr = record.amountCurr!!.toBigDecimal(),
                    amountLoc = record.amountLoc!!.toBigDecimal(),
                    taxableAmount = BigDecimal.ZERO,
                    payCurr = record.payCurr!!.toBigDecimal(),
                    payLoc = record.payLoc!!.toBigDecimal(),
                    dueDate = SimpleDateFormat("yyyy-MM-dd").parse(record.transactionDate),
                    transactionDate = SimpleDateFormat("yyyy-MM-dd").parse(record.transactionDate),
                    createdAt = record.createdAt,
                    updatedAt = record.updatedAt,
                    migrated = true,
                    isVoid = false,
                    taggedBillId = null,
                    tdsAmountLoc = BigDecimal.ZERO,
                    tdsAmount = BigDecimal.ZERO
                )
            )
            migrationLogService.saveMigrationLogs(null, null, record.documentValue, null, null, null, null, null, null, null)
        } catch (ex: AresException) {
            logger().info("${record.sageOrganizationId} Error while migrating newPR record")
            migrationLogService.saveMigrationLogs(null, null, record.documentValue, null, null, null, null, null, null, "${record.sageOrganizationId} : ${ex.context}")
        } catch (ex: Exception) {
            val message = "${record.sageOrganizationId}: Error while migrating newPR record"
            migrationLogService.saveMigrationLogs(null, null, record.documentValue, null, null, null, null, null, null, message)
        }
    }

    override suspend fun migrateJVUtilization(record: JVRecordsScheduler) {
        try {
            val organizationType = if (record.accMode == "AP") "expense" else "income"
            val tradePartyDetailId = cogoClient.getOrgDetailsBySageOrgId(
                GetOrgDetailsRequest(
                    sageOrganizationId = record.sageOrganizationId,
                    organizationType = organizationType
                )
            ).organizationTradePartyDetailId ?: throw AresException(AresError.ERR_1003, "organizationTradePartyDetailId not found")
            var migrationStatus = MigrationStatus.PAYLOC_UPDATED
            val accUtilId = journalVoucherRepoMigration.getAccUtilId(
                record.sageUniqueId!!,
                record.paymentNum!!,
                AccMode.valueOf(record.accMode!!),
                tradePartyDetailId
            ) ?: throw AresException(AresError.ERR_1003, "jv records not found in account_utilization")
            accountUtilizationRepositoryMigration.updateJVUtilizationAmount(
                accUtilId,
                record.accountUtilPayCurr!!.toBigDecimal(),
                record.accountUtilPayLed!!.toBigDecimal()
            )
            migrationLogService.saveMigrationLogs(null, null, null, record.paymentNum, migrationStatus)
        } catch (ex: AresException) {
            migrationLogService.saveMigrationLogs(
                null, null, ex.context,
                record.paymentNum, MigrationStatus.PAYLOC_NOT_UPDATED
            )
        } catch (ex: Exception) {
            logger().info("error message $ex")
            migrationLogService.saveMigrationLogs(
                null, null, "error while migrating jv utilization record",
                record.paymentNum, MigrationStatus.PAYLOC_NOT_UPDATED
            )
        }
    }

    private fun getFormattedNumber(record: String?): String {
        var numString = ""
        try {
            record?.forEach { ch ->
                if (ch.isDigit())
                    numString += ch
            }
            return numString
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        return "NA"
    }

    override suspend fun migrateSagePaymentNum(request: SagePaymentNumMigrationResponse) {
        val paymentDetails = paymentMigrationRepository.getPaymentFromSageRefNum(request.sageRefNum!!)
        if (!paymentDetails.toString().isNullOrEmpty()) {
            paymentMigrationRepository.updateSageRefNum(paymentDetails, request.sagePaymentNum!!)
        }
    }
    override suspend fun partialPaymentMismatchDocument(documentNo: Long) {
        try {
            val accountUtilization = accountUtilizationRepositoryMigration.findNonMigratedRecord(documentNo, accMode = AccMode.AP.name, accType = null)
            if (accountUtilization?.accType !in listOf(AccountType.PINV, AccountType.PREIMB)) {
                return
            }
            val ledgerExchangeRate = accountUtilization?.amountLoc!! / accountUtilization.amountCurr
            val settlementDetails = settlementRepository.getPaymentsCorrespondingDocumentNos(destinationId = documentNo, sourceId = null)
            val paidTdsAmount = settlementDetails.filter { it?.sourceType == SettlementType.VTDS }.sumOf { it?.amount ?: BigDecimal.ZERO }
            val payableAmount = accountUtilization.amountCurr - (accountUtilization.tdsAmount ?: BigDecimal.ZERO)
            val paidAmountWithoutTds = accountUtilization.payCurr - paidTdsAmount
            var leftAmount = payableAmount - paidAmountWithoutTds
            if (settlementDetails.isEmpty() || paidTdsAmount.compareTo(BigDecimal.ZERO) == 0 || leftAmount.compareTo(BigDecimal.ZERO) == 0) return
            val paymentDetails = paymentMigrationRepository.paymentDetailsByPaymentNum(settlementDetails.map { it?.sourceId!! }).filter { it.documentNo != null }
            var totalUnutilizedAmount = paymentDetails.sumOf { it.unutilisedAmount }
            paymentDetails.forEach { payment ->
                var amount = BigDecimal.ZERO
                if (payment.unutilisedAmount > BigDecimal.ZERO && leftAmount <= totalUnutilizedAmount && (accountUtilization.amountCurr > accountUtilization.payCurr) && leftAmount > BigDecimal.ZERO) {
                    amount = payment.unutilisedAmount.min(leftAmount)
                    accountUtilizationRepositoryMigration.updateSettlementAmount(documentNo, payment.paymentNum, amount, (amount * ledgerExchangeRate))
                    accountUtilizationRepositoryMigration.updateAccountUtilizationsAmount(payment.id, amount, amount * ledgerExchangeRate)
                    accountUtilizationRepositoryMigration.updateAccountUtilizationsAmount(accountUtilization.id!!, amount, amount * ledgerExchangeRate)
                }
                leftAmount -= amount
                totalUnutilizedAmount -= amount
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            throw e
        }
    }

    override suspend fun migrateTDSJV(jvParentDetail: JVParentDetails) {
        var jvParentRecord: ParentJournalVoucherMigration? = null
        var jvRecords: List<JournalVoucherRecord>? = null
        var parentJVId = parentJournalVoucherRepo.checkIfParentJVExists(jvParentDetail.jvNum, jvParentDetail.jvType)
        val jvRecordsWithoutBpr = sageServiceImpl.getTDSJVLineItemWithNoBPR(jvParentDetail.jvNum, jvParentDetail.jvType)
        try {
            jvRecords = sageServiceImpl.getTDSJournalVoucherFromSageCorrected(null, null, "'${jvParentDetail.jvNum}'", jvParentDetail.jvType)
            var sum = BigDecimal.ZERO
            jvRecords.forEach {
                sum += (it.accountUtilAmtLed * BigDecimal.valueOf(it.signFlag!!.toLong()))
            }
            jvRecordsWithoutBpr.forEach {
                sum += (it.ledgerAmount * it.signFlag)
            }
            if (sum.toBigInteger() != BigDecimal.ZERO.toBigInteger()) {
                migrationLogService.saveMigrationLogs(
                    null, null, jvParentDetail.jvNum, null, null,
                    null, null, null, null, "jv Sum is not zero"
                )
                return
            }
            if (parentJVId == null) {
                jvParentRecord = parentJournalVoucherRepo.save(
                    ParentJournalVoucherMigration(
                        id = null,
                        status = JVStatus.valueOf(jvParentDetail.jvStatus),
                        category = AccountType.VTDS.name,
                        jvNum = jvParentDetail.jvNum,
                        validityDate = jvParentDetail.validityDate,
                        createdAt = jvParentDetail.createdAt,
                        updatedAt = jvParentDetail.updatedAt,
                        createdBy = MigrationConstants.createdUpdatedBy,
                        updatedBy = MigrationConstants.createdUpdatedBy,
                        migrated = true,
                        currency = jvParentDetail.currency,
                        ledCurrency = jvParentDetail.ledgerCurrency,
                        exchangeRate = jvParentDetail.exchangeRate,
                        description = jvParentDetail.description,
                        jvCodeNum = AccountType.VTDS.name,
                        entityCode = jvRecords.firstOrNull()?.entityCode,
                        transactionDate = jvParentDetail.validityDate
                    )
                )
                parentJVId = jvParentRecord.id!!
                migrationLogService.saveMigrationLogs(
                    null, null, jvParentDetail.jvNum, jvParentDetail.currency,
                    jvParentDetail.amount, null, null,
                    null, null, null
                )
            }
        } catch (ex: Exception) {
            logger().error("$ex")
            migrationLogService.saveMigrationLogs(
                null, null, jvParentDetail.jvNum, null, null,
                null, null, null, null, "Error while storing jv header: ${ex.message}"
            )
            return
        }
        storeJVLineItems(jvRecordsWithoutBpr, parentJVId)
        jvRecords.forEach {
            it.accountType = AccountType.VTDS.name
            this.migrateJournalVoucher(it, parentJVId)
        }
    }

    override suspend fun migrateAdminJV(jvParentDetail: JVParentDetails) {
        var jvParentRecord: ParentJournalVoucherMigration? = null
        var jvRecords: List<JournalVoucherRecord>? = null
        var parentJVId = parentJournalVoucherRepo.checkIfParentJVExists(jvParentDetail.jvNum, jvParentDetail.jvType)
        val jvRecordsWithoutBpr = sageServiceImpl.getJVLineItemWithNoBPR(jvParentDetail.jvNum, jvParentDetail.jvType)
        try {
            jvRecords = sageServiceImpl.getJournalVoucherFromSageCorrected(null, null, "'${jvParentDetail.jvNum}'", jvParentDetail.jvType)
            if (parentJVId == null) {
                jvParentRecord = parentJournalVoucherRepo.save(
                    ParentJournalVoucherMigration(
                        id = null,
                        status = JVStatus.valueOf(jvParentDetail.jvStatus),
                        category = AccountTypeMapping.getAccountType(jvParentDetail.jvType),
                        jvNum = jvParentDetail.jvNum,
                        validityDate = jvParentDetail.validityDate,
                        createdAt = jvParentDetail.createdAt,
                        updatedAt = jvParentDetail.updatedAt,
                        createdBy = MigrationConstants.createdUpdatedBy,
                        updatedBy = MigrationConstants.createdUpdatedBy,
                        migrated = true,
                        currency = jvParentDetail.currency,
                        ledCurrency = jvParentDetail.ledgerCurrency,
                        exchangeRate = jvParentDetail.exchangeRate,
                        description = jvParentDetail.description,
                        jvCodeNum = jvParentDetail.jvCodeNum,
                        entityCode = jvRecords.firstOrNull()?.entityCode,
                        transactionDate = jvParentDetail.validityDate
                    )
                )
                parentJVId = jvParentRecord.id!!
                migrationLogService.saveMigrationLogs(
                    null, null, jvParentDetail.jvNum, jvParentDetail.currency,
                    jvParentDetail.amount, null, null,
                    null, null, null
                )
            }
        } catch (ex: Exception) {
            logger().error("$ex")
            migrationLogService.saveMigrationLogs(
                null, null, jvParentDetail.jvNum, null, null,
                null, null, null, null, "Error while storing jv header: ${ex.message}"
            )
            return
        }
        storeJVLineItems(jvRecordsWithoutBpr, parentJVId)
        jvRecords.forEach {
            this.migrateJournalVoucher(it, parentJVId)
        }
    }

    override suspend fun mismatchAmount(id: Long) {
        val mismatchData = paymentMigrationRepository.getMismatchLspPaymentCheck(id)
        paymentMigrationRepository.updateMismatchLspPaymentsCheck(mismatchData.amount, mismatchData.ledAmount, mismatchData.documentNo, mismatchData.documentValue)
    }
}
