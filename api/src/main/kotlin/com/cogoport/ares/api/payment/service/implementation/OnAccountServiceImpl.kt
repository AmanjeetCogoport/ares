package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.Validations
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.common.models.BankDetails
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.migration.model.SagePlatformPaymentHeader
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdsInput
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.AresDocument
import com.cogoport.ares.api.payment.entity.PaymentFile
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.mapper.OrgStatsMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.model.PushAccountUtilizationRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.AresDocumentRepository
import com.cogoport.ares.api.payment.repository.PaymentFileRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.sage.service.implementation.SageServiceImpl
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.ExcelUtils
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocType
import com.cogoport.ares.model.payment.DocumentSearchType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.MappingIdDetailRequest
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.OrgStatsResponseForCoeFinance
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.TradePartyDetailRequest
import com.cogoport.ares.model.payment.TradePartyOrganizationResponse
import com.cogoport.ares.model.payment.ValidateTradePartyRequest
import com.cogoport.ares.model.payment.enum.CogoBankAccount
import com.cogoport.ares.model.payment.enum.PaymentSageGLCodes
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.ARLedgerResponse
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.BulkUploadErrorResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.OnAccountWithUtrResponse
import com.cogoport.ares.model.payment.response.PlatformOrganizationResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVSageAccount
import com.cogoport.ares.model.settlement.enums.JVSageControls
import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Style
import com.cogoport.brahma.excel.utils.ExcelSheetReader
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.s3.client.S3Client
import com.cogoport.brahma.sage.SageException
import com.cogoport.brahma.sage.model.request.PaymentLineItem
import com.cogoport.brahma.sage.model.request.PaymentRequest
import com.cogoport.brahma.sage.model.request.SageResponse
import com.cogoport.plutus.model.invoice.GetUserRequest
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.context.annotation.Value
import io.sentry.Sentry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import org.json.XML
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil
import com.cogoport.brahma.sage.Client as SageClient

@Singleton
open class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var railsClient: RailsClient

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var accountUtilizationMapper: AccountUtilizationMapper

    @Inject
    lateinit var orgStatsConverter: OrgStatsMapper

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var kuberMessagePublisher: KuberMessagePublisher

    @Inject
    lateinit var auditService: AuditService
    @Inject
    lateinit var paymentFileRepository: PaymentFileRepository
    @Inject
    lateinit var s3Client: S3Client
    @Inject
    lateinit var aresDocumentRepository: AresDocumentRepository
    @Value("\${aws.s3.bucket}")
    private lateinit var s3Bucket: String

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var cogoBackLowLevelClient: CogoBackLowLevelClient

    @Inject
    lateinit var sageServiceImpl: SageServiceImpl

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    @Inject
    lateinit var util: Util

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    @Value("\${server.base-url}") // application-prod.yml path
    private lateinit var baseUrl: String

    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse {
        val query = util.toQueryString(request.query)
        val sortType = request.sortType ?: "Desc"
        val sortBy = request.sortBy ?: "createdAt"
        val pageLimit = request.pageLimit
        val page = request.page

        val entityCodes = when (request.entityType != null) {
            true -> when (request.entityType) {
                AresConstants.ENTITY_101 -> listOf(AresConstants.ENTITY_101, AresConstants.ENTITY_201, AresConstants.ENTITY_301, AresConstants.ENTITY_401)
                else -> listOf(request.entityType)
            }
            else -> null
        }

        val documentTypes = when (request.docType != null) {
            true -> {
                when (request.docType) {
                    DocType.TDS -> listOf(PaymentCode.CTDS.name, PaymentCode.VTDS.name)
                    DocType.RECEIPT -> listOf(PaymentCode.REC.name)
                    else -> listOf(PaymentCode.PAY.name)
                }
            }
            else -> null
        }

        val paymentsData = paymentRepository.getOnAccountList(
            request.currencyType,
            entityCodes,
            request.accMode,
            request.startDate,
            request.endDate,
            query,
            sortType,
            sortBy,
            pageLimit,
            page,
            documentTypes,
            request.paymentDocumentStatus
        )

        val updatedByIds = paymentsData
            ?.mapNotNull { it.createdBy?.toString() }
            ?.filterNot { it.isEmpty() }
            ?.distinct()
            ?.let { ArrayList(it) }

        val usersData = if (updatedByIds.isNullOrEmpty()) {
            emptyList()
        } else {
            authClient.getUsers(GetUserRequest(id = updatedByIds))
        }

        val updatedPaymentsData = paymentsData?.map { paymentData ->
            usersData?.firstOrNull { it.userId == paymentData.updatedBy }?.let { userData ->
                paymentData.copy(uploadedBy = userData.userName)
            } ?: paymentData
        }

        val totalRecords = paymentRepository.getOnAccountListCount(
            request.currencyType,
            entityCodes,
            request.accMode,
            request.startDate,
            request.endDate,
            query,
            documentTypes,
            request.paymentDocumentStatus
        )

        return AccountCollectionResponse(
            list = updatedPaymentsData,
            totalRecords = totalRecords,
            totalPage = ceil(totalRecords.toDouble() / request.pageLimit.toDouble()).toInt(),
            page = request.page
        )
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        val filterDateFromTs = Timestamp(dateFormat.parse(receivableRequest.paymentDate).time)
        receivableRequest.transactionDate = filterDateFromTs

        if (receivableRequest.transactionDate!! > Date()) {
            throw AresException(AresError.ERR_1009, "Transaction date can't be of future")
        }

        setPaymentAmounts(receivableRequest)
        val paymentId = createNonSuspensePaymentEntry(receivableRequest)

        return OnAccountApiCommonResponse(id = paymentId, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    private suspend fun createNonSuspensePaymentEntry(receivableRequest: Payment): Long {
        if (receivableRequest.accMode == null) receivableRequest.accMode = AccMode.AR

        if (receivableRequest.docType != DocType.TDS && receivableRequest.bankAccountNumber.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, "Bank Account")
        }

        var isUtrExit: Boolean? = false
        if (receivableRequest.accMode == AccMode.AR) {
            isUtrExit = paymentRepository.isARTransRefNumberExists(accMode = AccMode.AR.name, transRefNumber = receivableRequest.utr!!)
        }

        if (isUtrExit == true) {
            throw AresException(AresError.ERR_1537, "")
        }

        receivableRequest.signFlag = when (receivableRequest.docType == DocType.TDS) {
            true -> when (receivableRequest.accMode == AccMode.AR) {
                true -> SignSuffix.CTDS.sign
                else -> SignSuffix.VTDS.sign
            }
            else -> when (receivableRequest.accMode == AccMode.AR) {
                true -> SignSuffix.REC.sign
                else -> SignSuffix.PAY.sign
            }
        }

//        setOrganizations(receivableRequest)
//        setTradePartyOrganizations(receivableRequest)
        setTradePartyInfo(receivableRequest, null)

        val payment = paymentConverter.convertToEntity(receivableRequest)

        setPaymentEntity(payment, receivableRequest.docType)
        payment.paymentDocumentStatus = payment.paymentDocumentStatus ?: PaymentDocumentStatus.CREATED
        val savedPayment = paymentRepository.save(payment)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PAYMENTS,
                objectId = savedPayment.id,
                actionName = AresConstants.CREATE,
                data = savedPayment,
                performedBy = receivableRequest.createdBy,
                performedByUserType = receivableRequest.performedByUserType
            )
        )
        receivableRequest.id = savedPayment.id

        receivableRequest.serviceType = ServiceType.NA
        receivableRequest.paymentNum = payment.paymentNum
        receivableRequest.paymentNumValue = payment.paymentNumValue
        receivableRequest.accCode = payment.accCode
        receivableRequest.paymentCode = payment.paymentCode
        receivableRequest.createdAt = savedPayment.createdAt
        receivableRequest.updatedAt = savedPayment.updatedAt

        val accUtilizationModel: AccUtilizationRequest = accUtilizationToPaymentConverter.convertEntityToModel(payment)

        setAccountUtilizationModel(accUtilizationModel, receivableRequest)

        val accUtilEntity = accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel)

        accUtilEntity.documentNo = payment.paymentNum!!
        accUtilEntity.documentValue = payment.paymentNumValue
        accUtilEntity.taxableAmount = BigDecimal.ZERO
        accUtilEntity.tdsAmount = BigDecimal.ZERO
        accUtilEntity.tdsAmountLoc = BigDecimal.ZERO

        accUtilEntity.accCode = when (receivableRequest.docType == DocType.TDS) {
            true -> {
                when (receivableRequest.accMode == AccMode.AR) {
                    true -> AresModelConstants.TDS_AR_ACCOUNT_CODE
                    false -> AresModelConstants.TDS_AP_ACCOUNT_CODE
                }
            }
            else -> {
                when (receivableRequest.accMode == AccMode.AR) {
                    true -> AresModelConstants.AR_ACCOUNT_CODE
                    false -> AresModelConstants.AP_ACCOUNT_CODE
                }
            }
        }

        if (receivableRequest.docType == DocType.TDS) {
            accUtilEntity.isVoid = false
            accUtilEntity.tdsAmountLoc = BigDecimal.ZERO
            accUtilEntity.tdsAmount = BigDecimal.ZERO
        }

        val accUtilRes = accountUtilizationRepository.save(accUtilEntity)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilRes.id,
                actionName = AresConstants.CREATE,
                data = accUtilRes,
                performedBy = receivableRequest.createdBy,
                performedByUserType = receivableRequest.performedByUserType
            )
        )
        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            if (accUtilRes.accMode == AccMode.AP) {
                aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilRes.organizationId))
            }
            if (accUtilRes.accMode == AccMode.AR) {
                aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilRes.organizationId))
            }
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
            Sentry.captureException(ex)
        }
        return savedPayment.id!!
    }

    private suspend fun setTradePartyOrganizations(receivableRequest: Payment) {
        val clientResponse: TradePartyOrganizationResponse?

        val reqBody = TradePartyDetailRequest(
            receivableRequest.organizationId?.toString(),
            receivableRequest.orgSerialId,
            AresConstants.PAYING_PARTY
        )

        clientResponse = authClient.getTradePartyDetailInfo(reqBody)

        if (clientResponse.organizationTradePartySerialId == null) {
            throw AresException(AresError.ERR_1207, "")
        }
        receivableRequest.orgSerialId = clientResponse.organizationTradePartySerialId
        receivableRequest.organizationName = clientResponse.organizationTradePartyName
        receivableRequest.zone = clientResponse.organizationTradePartyZone?.uppercase()
        receivableRequest.organizationId = clientResponse.organizationTradePartyDetailId
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data
     * to update Dashboard and Receivables outstanding documents on OpenSearch
     * @param accUtilizationRequest
     */
    private suspend fun emitDashboardAndOutstandingEvent(accUtilizationRequest: AccUtilizationRequest) {
        val date = accUtilizationRequest.dueDate ?: accUtilizationRequest.transactionDate
        aresMessagePublisher.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    orgId = accUtilizationRequest.organizationId.toString(),
                    orgName = accUtilizationRequest.organizationName
                )
            )
        )
    }

    /**
     * @param Payment
     * @return Payment
     */
    override suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        val accMode = receivableRequest.accMode?.name ?: throw AresException(AresError.ERR_1003, "accMode")

        if (receivableRequest.transactionDate != null && receivableRequest.transactionDate!! > Date()) {
            throw AresException(AresError.ERR_1009, "Transaction date can't be of future")
        }

//        receivableRequest.updatedBy ?: throw AresException(AresError.ERR_1003, "updatedBy")

        val accType = receivableRequest.paymentCode?.name ?: throw AresException(AresError.ERR_1003, "paymentCode")
        val payment = receivableRequest.id?.let { paymentRepository.findByPaymentId(it) } ?: throw AresException(AresError.ERR_1002, "Payment")

        if (payment.paymentDocumentStatus == PaymentDocumentStatus.APPROVED) throw AresException(AresError.ERR_1010, "")
        val accountUtilization = accountUtilizationRepository.findRecord(payment.paymentNum!!, accType, accMode) ?: throw AresException(AresError.ERR_1002, "Account Utilization")
        return updateNonSuspensePayment(receivableRequest, accountUtilization, payment)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updateNonSuspensePayment(receivableRequest: Payment, accountUtilizationEntity: AccountUtilization, paymentEntity: com.cogoport.ares.api.payment.entity.Payment): OnAccountApiCommonResponse {

        if (receivableRequest.paymentDocumentStatus != null && receivableRequest.paymentDocumentStatus == PaymentDocumentStatus.APPROVED) {
            accountUtilizationEntity.documentStatus = DocumentStatus.FINAL
            paymentEntity.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
            accountUtilizationEntity.settlementEnabled = true
        } else {

//            setOrganizations(receivableRequest)
//            setTradePartyOrganizations(receivableRequest)
            setTradePartyInfo(receivableRequest, paymentEntity)

            /*SET PAYMENT ENTITY DATA FOR UPDATE*/
            updatePaymentEntity(receivableRequest, paymentEntity)

            /*SET ACCOUNT UTILIZATION DATA FOR UPDATE*/
            updateAccountUtilizationEntity(receivableRequest, accountUtilizationEntity)
            accountUtilizationEntity.transactionDate = paymentEntity.transactionDate
        }

        /*UPDATE THE DATABASE WITH UPDATED PAYMENT ENTRY*/
        val paymentDetails = paymentRepository.update(paymentEntity)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PAYMENTS,
                objectId = paymentEntity.id,
                actionName = AresConstants.UPDATE,
                data = paymentEntity,
                performedBy = receivableRequest.createdBy.toString(),
                performedByUserType = receivableRequest.performedByUserType
            )
        )

        /*UPDATE THE DATABASE WITH UPDATED ACCOUNT UTILIZATION ENTRY*/
        val accUtilRes = accountUtilizationRepository.update(accountUtilizationEntity)

        if ((paymentDetails.entityCode != 501) && (paymentDetails.paymentCode in listOf(PaymentCode.REC, PaymentCode.CTDS))) {
//            aresMessagePublisher.emitPostPaymentToSage(
//                PostPaymentToSage(
//                    paymentId = paymentEntity.id!!,
//                    performedBy = paymentEntity.updatedBy!!
//                )
//            )
            try {
                directFinalPostToSage(
                    PostPaymentToSage(
                        paymentId = paymentEntity.id!!,
                        performedBy = paymentEntity.updatedBy!!

                    )
                )
            } catch (ex: Exception) {
                logger().info(ex.stackTraceToString())
            }
        }

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accountUtilizationEntity.id,
                actionName = AresConstants.UPDATE,
                data = accountUtilizationEntity,
                performedBy = receivableRequest.createdBy.toString(),
                performedByUserType = receivableRequest.performedByUserType
            )
        )

        try {
            /*UPDATE THE OPEN SEARCH WITH UPDATED ACCOUNT UTILIZATION ENTRY */
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            // EMITTING KAFKA MESSAGE TO UPDATE OUTSTANDING and DASHBOARD
            emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))
            // EMITTING RABITMQ MESSAGE TO UPDATE CUSTOMER OUTSTANDING
            if (accUtilRes.accMode == AccMode.AR) {
                aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilizationEntity.organizationId))
            }
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }
        return OnAccountApiCommonResponse(id = accUtilRes.id!!, message = Messages.PAYMENT_UPDATED, isSuccess = true)
    }

    private fun updatePaymentEntity(receivableRequest: Payment, paymentEntity: com.cogoport.ares.api.payment.entity.Payment) {
        val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        val paymentDate = if (receivableRequest.paymentDate != null) {
            dateFormat.parse(receivableRequest.paymentDate)
        } else {
            paymentEntity.transactionDate
        }
        paymentEntity.entityCode = receivableRequest.entityType ?: paymentEntity.entityCode
        paymentEntity.bankName = receivableRequest.bankName ?: paymentEntity.bankName
        paymentEntity.payMode = receivableRequest.payMode ?: paymentEntity.payMode
        paymentEntity.transactionDate = paymentDate
        paymentEntity.transRefNumber = receivableRequest.utr ?: paymentEntity.transRefNumber
        paymentEntity.amount = receivableRequest.amount ?: paymentEntity.amount
        paymentEntity.currency = receivableRequest.currency ?: paymentEntity.currency
        paymentEntity.ledAmount = (receivableRequest.amount?.times(receivableRequest.exchangeRate!!)) ?: paymentEntity.ledAmount
        paymentEntity.ledCurrency = receivableRequest.ledCurrency ?: paymentEntity.ledCurrency
        paymentEntity.exchangeRate = receivableRequest.exchangeRate ?: paymentEntity.exchangeRate
        paymentEntity.orgSerialId = receivableRequest.orgSerialId ?: paymentEntity.orgSerialId
        paymentEntity.organizationId = receivableRequest.organizationId ?: paymentEntity.organizationId
        paymentEntity.organizationName = receivableRequest.organizationName ?: paymentEntity.organizationName
        paymentEntity.bankName = receivableRequest.bankName ?: paymentEntity.bankName
        paymentEntity.cogoAccountNo = receivableRequest.bankAccountNumber ?: paymentEntity.cogoAccountNo
        paymentEntity.updatedAt = Timestamp.from(Instant.now())
        paymentEntity.bankId = receivableRequest.bankId ?: paymentEntity.bankId
        paymentEntity.updatedBy = UUID.fromString(receivableRequest.updatedBy) ?: paymentEntity.updatedBy
    }

    private fun updateAccountUtilizationEntity(receivableRequest: Payment, accountUtilizationEntity: AccountUtilization) {
        accountUtilizationEntity.entityCode = receivableRequest.entityType ?: accountUtilizationEntity.entityCode
        accountUtilizationEntity.orgSerialId = receivableRequest.orgSerialId ?: accountUtilizationEntity.orgSerialId
        accountUtilizationEntity.organizationId = receivableRequest.organizationId ?: accountUtilizationEntity.organizationId
        accountUtilizationEntity.organizationName = (receivableRequest.organizationName ?: accountUtilizationEntity.organizationName).toString()
        accountUtilizationEntity.amountCurr = receivableRequest.amount ?: accountUtilizationEntity.amountCurr
        accountUtilizationEntity.currency = receivableRequest.currency ?: accountUtilizationEntity.currency
        accountUtilizationEntity.amountLoc = receivableRequest.ledAmount ?: accountUtilizationEntity.amountLoc
        accountUtilizationEntity.ledCurrency = receivableRequest.ledCurrency ?: accountUtilizationEntity.ledCurrency
        accountUtilizationEntity.updatedAt = Timestamp.from(Instant.now())
        accountUtilizationEntity.zoneCode = receivableRequest.zone ?: accountUtilizationEntity.zoneCode
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse {
        try {
            val payment = paymentRepository.findByPaymentId(deletePaymentRequest.paymentId)
            if (payment.deletedAt != null) throw AresException(AresError.ERR_1007, "")

            val settlement = settlementRepository.findBySourceIdAndSourceType(payment.paymentNum!!, listOf(SettlementType.valueOf(payment.paymentCode?.name!!)))
            logger().info("settlementDetails: $settlement")
            if (!settlement.isNullOrEmpty()) throw AresException(AresError.ERR_1540, "Payment is already utilized.")

            payment.deletedAt = Timestamp.from(Instant.now())
            payment.updatedAt = payment.deletedAt
            payment.paymentDocumentStatus = PaymentDocumentStatus.DELETED
            payment.updatedBy = when (deletePaymentRequest.performedById == null) {
                true -> payment.createdBy
                false -> UUID.fromString(deletePaymentRequest.performedById)
            }
            /*MARK THE PAYMENT AS DELETED IN DATABASE*/
            val paymentResponse = paymentRepository.update(payment)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.PAYMENTS,
                    objectId = payment.id,
                    actionName = AresConstants.DELETE,
                    data = payment,
                    performedBy = deletePaymentRequest.performedById,
                    performedByUserType = deletePaymentRequest.performedByUserType
                )
            )

            val accType = AccountType.valueOf(payment.paymentCode?.name!!)

            val accountUtilization = accountUtilizationRepository.findRecord(
                payment.paymentNum!!, accType.name, deletePaymentRequest.accMode?.name
            ) ?: throw AresException(AresError.ERR_1005, "")
            accountUtilization.documentStatus = DocumentStatus.DELETED
            accountUtilization.settlementEnabled = false
            accountUtilization.deletedAt = Timestamp.valueOf(LocalDateTime.now())
            accountUtilization.updatedAt = accountUtilization.deletedAt

            /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN DATABASE*/
            val accUtilRes = accountUtilizationRepository.update(accountUtilization)

            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                    objectId = accountUtilization.id,
                    actionName = AresConstants.DELETE,
                    data = accountUtilization,
                    performedBy = deletePaymentRequest.performedById,
                    performedByUserType = deletePaymentRequest.performedByUserType
                )
            )
//            if (payment.paymentDocumentStatus == PaymentDocumentStatus.APPROVED && payment.paymentCode == PaymentCode.PAY) {
//                val request = DeleteSettlementRequest(
//                    documentNo = Hashids.encode(payment.paymentNum!!),
//                    deletedBy = UUID.fromString(deletePaymentRequest.performedById),
//                    deletedByUserType = deletePaymentRequest.performedByUserType,
//                    settlementType = SettlementType.PAY
//                )
//                settlementService.delete(request)
//            }
            try {
                /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN OPEN SEARCH*/
                Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes, true)
                // Emitting Kafka message to Update Outstanding and Dashboard
                emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))
                // Emitting RabbitMq message to Update Customer Outstanding
                if (accountUtilization.accMode == AccMode.AR) {
                    aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilization.organizationId))
                }
            } catch (ex: Exception) {
                logger().error(ex.stackTraceToString())
            }
        } catch (aresException: AresException) {
            logger().error("""${mapOf("paymentId" to deletePaymentRequest.paymentId, "error" to "${aresException.error.message} ${aresException.context}")}""")
            throw aresException
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            Sentry.captureException(e)
            throw e
        }
        return OnAccountApiCommonResponse(id = deletePaymentRequest.paymentId, message = Messages.PAYMENT_DELETED, isSuccess = true)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse {
        var recordsInserted = 0
        for (payment in bulkPayment) {
            setBankDetails(payment)

            val onAccountApiCommonResponse = createPaymentEntry(payment)
            if (onAccountApiCommonResponse.isSuccess)
                recordsInserted++
        }
        return BulkPaymentResponse(recordsInserted = recordsInserted)
    }

    private fun setPaymentAmounts(payment: Payment) {
        if (payment.currency == payment.ledCurrency) {
            payment.ledAmount = payment.amount
            payment.exchangeRate = BigDecimal.ONE
        } else {
            payment.ledAmount = payment.amount!! * payment.exchangeRate!!
        }
    }

    private suspend fun setPaymentEntity(payment: com.cogoport.ares.api.payment.entity.Payment, docType: DocType?) {
        val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
        when (docType == DocType.TDS) {
            true -> {
                if (payment.accMode == AccMode.AR) {
                    payment.accCode = AresModelConstants.TDS_AR_ACCOUNT_CODE
                    payment.paymentCode = PaymentCode.CTDS
                    payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.CTDS.prefix)
                    payment.paymentNumValue = payment.paymentCode.toString() + financialYearSuffix + payment.paymentNum
                } else {
                    payment.accCode = AresModelConstants.TDS_AP_ACCOUNT_CODE
                    payment.paymentCode = PaymentCode.VTDS
                    payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.VTDS.prefix)
                    payment.paymentNumValue = payment.paymentCode.toString() + financialYearSuffix + payment.paymentNum
                }
            }
            else -> {
                if (payment.accMode == AccMode.AR) {
                    payment.accCode = AresModelConstants.AR_ACCOUNT_CODE
                    payment.paymentCode = PaymentCode.REC
                    payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
                    payment.paymentNumValue = SequenceSuffix.RECEIVED.prefix + financialYearSuffix + payment.paymentNum
                } else {
                    payment.accCode = AresModelConstants.AP_ACCOUNT_CODE
                    payment.paymentCode = PaymentCode.PAY
                    payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.PAYMENT.prefix)
                    payment.paymentNumValue = SequenceSuffix.PAYMENT.prefix + financialYearSuffix + payment.paymentNum
                }
            }
        }

        payment.migrated = false
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())
    }

    private fun setAccountUtilizationModel(accUtilizationModel: AccUtilizationRequest, receivableRequest: Payment) {
        accUtilizationModel.accType = when (receivableRequest.docType == DocType.TDS) {
            true -> {
                when (receivableRequest.accMode == AccMode.AR) {
                    true -> AccountType.CTDS
                    else -> AccountType.VTDS
                }
            }
            else -> {
                when (receivableRequest.accMode == AccMode.AR) {
                    true -> AccountType.REC
                    else -> AccountType.PAY
                }
            }
        }
        accUtilizationModel.zoneCode = receivableRequest.zone
        accUtilizationModel.serviceType = receivableRequest.serviceType
        accUtilizationModel.currencyPayment = BigDecimal.ZERO
        accUtilizationModel.ledgerPayment = BigDecimal.ZERO
        accUtilizationModel.ledgerAmount = receivableRequest.ledAmount
        accUtilizationModel.ledCurrency = receivableRequest.ledCurrency!!
        accUtilizationModel.currency = receivableRequest.currency!!
        accUtilizationModel.docStatus = when (receivableRequest.paymentDocumentStatus in listOf(PaymentDocumentStatus.APPROVED, PaymentDocumentStatus.POSTED, PaymentDocumentStatus.POSTING_FAILED, PaymentDocumentStatus.FINAL_POSTED)) {
            true -> DocumentStatus.FINAL
            false -> DocumentStatus.PROFORMA
        }
        accUtilizationModel.migrated = false
        accUtilizationModel.settlementEnabled = when (receivableRequest.paymentDocumentStatus in listOf(PaymentDocumentStatus.APPROVED, PaymentDocumentStatus.POSTED, PaymentDocumentStatus.POSTING_FAILED, PaymentDocumentStatus.FINAL_POSTED)) {
            true -> true
            false -> false
        }
    }

    private suspend fun setOrganizations(receivableRequest: Payment) {
        val clientResponse: PlatformOrganizationResponse?

        val reqBody = CogoOrganizationRequest(
            receivableRequest.organizationId?.toString(),
            receivableRequest.orgSerialId,
        )

        clientResponse = authClient.getCogoOrganization(reqBody)

        if (clientResponse.organizationSerialId == null) {
            throw AresException(AresError.ERR_1207, "")
        }
        receivableRequest.orgSerialId = clientResponse.organizationSerialId
        receivableRequest.organizationName = clientResponse.organizationName
        receivableRequest.zone = clientResponse.zone?.uppercase()
        receivableRequest.organizationId = clientResponse.organizationId
    }

    override suspend fun getOrganizationAccountUtlization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?> {
        val data = OpenSearchClient().onAccountUtilizationSearch(request, AccountUtilizationResponse::class.java)!!
        return data.hits().hits().map { it.source() }
    }

    private suspend fun setBankDetails(payment: Payment) {
        val bankDetails = authClient.getCogoBank(
            CogoEntitiesRequest(
                payment.entityType!!.toString()
            )
        )

        for (bankList in bankDetails.bankList) {
            for (bankInfo in bankList.bankDetails!!) {
                if (payment.bankAccountNumber.equals(bankInfo.accountNumber, ignoreCase = true)) {
                    payment.bankName = bankInfo.beneficiaryName
                    payment.bankId = bankInfo.id
                    return
                }
            }
        }
    }

    override suspend fun onAccountTotalAmountService(req: OnAccountTotalAmountRequest): MutableList<OnAccountTotalAmountResponse> {
        val res = accountUtilizationRepository.onAccountPaymentAmount(req.accType, req.accMode, req.orgIdList)
        return res
    }

    override suspend fun getOrgStats(orgId: UUID?): OrgStatsResponse {
        if (orgId == null) throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        val response = accountUtilizationRepository.getOrgStats(orgId) ?: throw AresException(AresError.ERR_1005, "")
        return orgStatsConverter.convertToModel(response)
    }

    override suspend fun getOrgStwatsForCoeFinance(orgId: UUID?): OrgStatsResponseForCoeFinance {
        if (orgId == null) throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        val payableStats = accountUtilizationRepository.getOrgStatsForCoeFinance(orgId) ?: throw AresException(AresError.ERR_1005, "")
        val tradePartyOutstandingList = cogoBackLowLevelClient.getTradePartyOutstanding(orgId.toString(), "get_trade_party_outstanding")
        var totalOutstandingAmount: BigDecimal? = null
        var outstandingCurrency: String? = null
        if (tradePartyOutstandingList?.list?.size != 0) {
            totalOutstandingAmount = tradePartyOutstandingList?.list?.get(0)?.totalOutstanding?.invoiceLedAmount
            outstandingCurrency = tradePartyOutstandingList?.list?.get(0)?.currency
        }
        val orgStatsResponse = OrgStatsResponseForCoeFinance(
            organizationId = orgId.toString(),
            receivables = totalOutstandingAmount,
            receivablesCurrency = outstandingCurrency,
            payablesCurrency = payableStats.ledgerCurrency,
            payables = payableStats.payables?.abs()
        )
        return orgStatsResponse
    }

    private suspend fun setTradePartyInfo(receivableRequest: Payment, paymentDetails: com.cogoport.ares.api.payment.entity.Payment?) {
        val clientResponse: TradePartyOrganizationResponse?

        val tradePartyMappingId = receivableRequest.tradePartyMappingId ?: paymentDetails?.tradePartyMappingId

        val reqBody = MappingIdDetailRequest(
            tradePartyMappingId.toString()
        )

        clientResponse = authClient.getTradePartyInfo(reqBody)

        if (clientResponse.organizationTradePartySerialId == null) {
            throw AresException(AresError.ERR_1207, "")
        }
        receivableRequest.orgSerialId = clientResponse.organizationTradePartySerialId
        receivableRequest.organizationName = clientResponse.organizationTradePartyName
        receivableRequest.zone = clientResponse.organizationTradePartyZone?.uppercase() ?: "EAST"
        receivableRequest.organizationId = clientResponse.organizationTradePartyDetailId
    }

    override suspend fun getDataAccUtilization(request: PushAccountUtilizationRequest): List<AccountUtilization> {
        val accUtilizationResponse: MutableList<AccountUtilization> = mutableListOf()
        var accUtilizationEntity: AccountUtilization
        for (accUtilizationReq in request.accountUtilizations) {
            if ((request.inputType) == DocumentSearchType.NUMBER) {
                accUtilizationEntity = accountUtilizationRepository.getAccountUtilizationsByDocNo(accUtilizationReq.id, accUtilizationReq.accType)
            } else {
                accUtilizationEntity = accountUtilizationRepository.getAccountUtilizationsByDocValue(accUtilizationReq.id, accUtilizationReq.accType)
            }
            accUtilizationResponse.add(accUtilizationEntity)
            Client.addDocument("test_invoices", accUtilizationEntity.id.toString(), accUtilizationEntity)
        }
        return accUtilizationResponse
    }

    override suspend fun deleteConsolidatedInvoices(req: DeleteConsolidatedInvoicesReq) {
        accountUtilizationRepository.deleteConsolidatedInvoices(req.docValues)
        auditService.createAudit(
            AuditRequest(
                actionName = "DELETE",
                objectId = req.jobId,
                objectType = "jobs",
                data = req.docValues,
                performedBy = req.performedBy,
                performedByUserType = req.performedByUserType
            )
        )
    }

    override suspend fun onAccountBulkAPPayments(request: BulkUploadRequest): UploadSummary {
        val excelFile = downloadExcelFile(request.fileUrl)
        val excelSheetReader = ExcelSheetReader(excelFile)
        val paymentData = excelSheetReader.read()
        val noOfColumns = paymentData.first().size
        excelFile.delete()
        if (noOfColumns != 12) {
            throw AresException(AresError.ERR_1511, "Number of columns mismatch")
        }

        if (paymentData.isEmpty()) {
            throw AresException(AresError.ERR_1511, "No Data found!")
        }

        val fileName: String = request.fileUrl.substring(request.fileUrl.lastIndexOf('/') + 1)
        val paymentFile = PaymentFile(
            id = null,
            fileName = fileName,
            accMode = AccMode.AP,
            fileUrl = request.fileUrl,
            successRecords = 0,
            totalRecords = 0,
            createdBy = request.uploadedBy,
            updatedBy = request.uploadedBy,
            errorFileUrl = null,
            createdAt = Timestamp(System.currentTimeMillis()),
            updatedAt = Timestamp(System.currentTimeMillis())
        )
        var result = paymentFileRepository.save(paymentFile)
        var (paymentModelList: kotlin.collections.MutableList<com.cogoport.ares.model.payment.Payment>, fileStats, fileUrl) =
            readAndValidateExcel(paymentData, fileName, request.uploadedBy)

        var (totalCount, successCount) = fileStats

        for (payment in paymentModelList) {
            payment.paymentDate = Utilities.getDateFromString(payment.paymentDate!!)
        }

        var recordsInserted = 0
        if (successCount != 0 && paymentModelList.size > 0) {
            val res = createBulkPayments(paymentModelList)
            recordsInserted = res.recordsInserted
        }

        if (recordsInserted != 0)
            successCount = recordsInserted

        result.errorFileUrl = fileUrl
        result.totalRecords = totalCount
        result.successRecords = successCount
        paymentFileRepository.update(result)

        var document: AresDocument? = null
        if (totalCount != successCount && fileUrl.isNotBlank()) {
            document = AresDocument(
                documentUrl = fileUrl,
                documentName = fileName,
                documentType = "AP_Upload_Error_list",
                uploadedBy = request.uploadedBy
            )
            aresDocumentRepository.save(document)
        }

        return UploadSummary(errorFileUrlId = document?.id, successCount, totalCount - successCount)
    }

    private fun downloadExcelFile(fileUrl: String): File {
        val httpClient: HttpClient = HttpClient.newBuilder().build()

        val httpRequest: HttpRequest = HttpRequest
            .newBuilder()
            .uri(URI(fileUrl))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = httpClient
            .send(httpRequest) { HttpResponse.BodySubscribers.ofInputStream() }
        val fileNme = fileUrl.split("/").last()
        val file = File(fileNme)
        val inp: InputStream = ByteArrayInputStream(response.body().readAllBytes())
        val path = Paths.get(file.path) as Path
        val result = Files.copy(inp, path, StandardCopyOption.REPLACE_EXISTING)
        return file
    }

    private suspend fun readAndValidateExcel(paymentData: List<Map<String, Any>>, fileName: String, uploadedBy: UUID): Triple<MutableList<Payment>, Pair<Int, Int>, String> {
        var paymentList = mutableListOf<Payment>()
        var paymentResponseList = mutableListOf<BulkUploadErrorResponse>()
        var hasErrors = false
        var errorCount = 0
        var recordCount = 0
        val cogoEntities = authClient.getCogoBank(CogoEntitiesRequest(""))
        val allEntityTypes = cogoEntities.bankList.map { it.entityCode }
        val orgTradeSerialIdMap = mutableListOf<SerialIdsInput>()
        paymentData.forEach {
            val input = SerialIdsInput(
                organizationSerialId = it["organization_serial_id"].toString().toLong(),
                tradePartyDetailSerialId = it["trade_party_serial_id"].toString().toLong(),
                cogoEntityId = AresConstants.ENTITY_ID[it["entity_code"].toString().toInt()]
            )
            if (input !in orgTradeSerialIdMap) orgTradeSerialIdMap.add(input)
        }
        val serialClientResponse = authClient.getSerialIdDetails(
            SerialIdDetailsRequest(
                organizationTradePartyMappings = orgTradeSerialIdMap
            )
        )

        val uploadedByName = authClient.getUsers(
            GetUserRequest(
                id = arrayListOf(uploadedBy.toString())
            )
        )

        paymentData.forEach {
            val errors = StringBuilder()
            hasErrors = false
            recordCount++

            val organizationSerialNo = it["organization_serial_id"].toString()
            val tradePartySerialNo = it["trade_party_serial_id"].toString()
            val entityCode = it["entity_code"].toString()
            val accountNumber = it["cogo_account_no"].toString()
            val payMode = it["pay_mode"].toString()
            val currency = it["currency"].toString()
            val paymentDate = it["payment_date"].toString()
            val ledgerCurrency = cogoEntities.bankList.find { det -> det.entityCode.toString() == it["entity_code"].toString() }?.ledgerCurrency
            val utr = it["utr"].toString()
            val serialIdDetails = serialClientResponse?.find { detail ->
                detail?.organization?.orgSerialId == organizationSerialNo.toLong() && detail.tradePartySerial.toString() == tradePartySerialNo
            }

            var payModeValue: PayMode? = null
            val bankAccounts = cogoEntities.bankList.find { it.entityCode.toString() == entityCode }?.bankDetails!!.map { it.accountNumber }
            var clientResponse: PlatformOrganizationResponse? = null
            var bankId = ""

            if (!Validations.validateUTR(utr)) {
                errors.append(" Invalid UTR No,")
                hasErrors = true
            }

            if (!Validations.checkForCurrency(currency)) {
                errors.append(" Invalid Currency,")
                hasErrors = true
            }

            if (!Validations.checkForNumeral(it.get("amount").toString())) {
                errors.append(" Invalid Amount")
                hasErrors = true
            }

            if (ledgerCurrency == null) {
                hasErrors = true
                errors.append("ledger currency for this entitiy type does not exist")
            }

            if (payMode.isNullOrEmpty()) {
                hasErrors = true
                errors.append("Payment Mode is empty")
            } else {
                try {
                    if (!it["pay_mode"].toString().isNullOrEmpty()) {
                        payModeValue = PayMode.valueOf(payMode)
                    }
                } catch (e: Exception) {
                    hasErrors = true
                    errors.append("Invalid Enum Format for pay_mode")
                }
            }

            if (tradePartySerialNo.isNullOrEmpty()) {
                errors.append("Trade Party Serial No is empty")
                hasErrors = true
            } else {
                val response = authClient.validateTradeParty(
                    ValidateTradePartyRequest(
                        serialId = organizationSerialNo
                    )
                )
                if (response == null || response == false) {
                    hasErrors = true
                    errors.append("Invalid Trade Party Serial No")
                }
            }

            if (organizationSerialNo.isNullOrEmpty()) {
                errors.append("Organization Serial No is empty")
                hasErrors = true
            } else {
                if (serialIdDetails == null) {
                    hasErrors = true
                    errors.append("Mapping between Organization and Trade Party does not exist")
                }
            }

            if (entityCode.isNullOrEmpty()) {
                hasErrors = true
                errors.append("Entity Type is empty")
            } else if (
                !allEntityTypes.contains(entityCode.toInt())
            ) {
                hasErrors = true
                errors.append("Invalid Entity Type")
            }

            if (accountNumber.isNullOrEmpty()) {
                hasErrors = true
                errors.append("Account Number is Empty")
            } else if (!bankAccounts.contains(accountNumber)) {
                hasErrors = true
                errors.append("Account Number is not associated with entity type provided")
            }

            if (paymentDate.isNullOrEmpty()) {
                hasErrors = true
                errors.append("Uploaded Date is Empty")
            } else {
                try {
                    var uploadedDate = Utilities.getTimeStampFromString(paymentDate)
                } catch (e: Exception) {
                    hasErrors = true
                    errors.append("Invalid Date Format")
                }
            }

            if (!hasErrors) {
                hasErrors = true
                val payableBankDetails = mutableListOf<BankDetails>()
                for (bankList in cogoEntities.bankList) {
                    bankList.bankDetails?.forEach {
                        if (it.canPay == true && bankList.entityCode.toString() == entityCode)
                            payableBankDetails.add(it)
                    }
                }

                for (bankDetail in payableBankDetails) {
                    if (
                        bankDetail.accountNumber.equals(accountNumber) &&
                        bankDetail.currency.equals(currency)
                    ) {
                        hasErrors = false
                        bankId = bankDetail.id.toString()
                    }
                }

                if (hasErrors) {
                    hasErrors = true
                    errors.append("Invalid Bank Details")
                }
            }

            var amount = 0.toBigDecimal()
            var ledAmount = 0.toBigDecimal()

            try {
                if (!it["amount"].toString().isNullOrEmpty()) {
                    amount = it["amount"].toString().toBigDecimal()
                }

                if (!it["exchange_rate"].toString().isNullOrEmpty()) {
                    ledAmount = amount.multiply(it["exchange_rate"].toString().toBigDecimal())
                }
            } catch (ex: NumberFormatException) {
                hasErrors = true
                errors.append("Invalid Number Format")
            }

            val paymentObj = Payment(
                organizationName = serialIdDetails?.tradePartyBusinessName,
                orgSerialId = it["trade_party_serial_id"].toString().toLong(),
                entityType = if (!it["entity_code"].toString().isNullOrEmpty()) it.get("entity_code").toString().toInt() else 0,
                bankAccountNumber = it["cogo_account_no"].toString(),
                refAccountNo = it["ref_account_no"].toString(),
                amount = amount,
                currency = it["currency"].toString(),
                utr = it["utr"].toString(),
                remarks = it["remarks"].toString(),
                accMode = AccMode.AP,
                signFlag = 1,
                paymentCode = PaymentCode.PAY,
                payMode = payModeValue,
                ledAmount = ledAmount,
                ledCurrency = ledgerCurrency,
                paymentNum = 0L,
                organizationId = serialIdDetails?.organizationTradePartyDetailId,
                paymentNumValue = null,
                serviceType = ServiceType.NA,
                bankId = null,
                paymentDate = paymentDate,
                createdBy = uploadedBy.toString(),
                updatedBy = uploadedBy.toString(),
                uploadedBy = uploadedByName?.get(0)?.userName,
                tradePartyMappingId = serialIdDetails?.mappingId,
                taggedOrganizationId = serialIdDetails?.organizationId,
                paymentDocumentStatus = PaymentDocumentStatus.CREATED,
                docType = DocType.PAYMENT
            )

            if (hasErrors) {
                val s3PaymentResponse = getS3PaymentResponse(it, errors)
                if (s3PaymentResponse.errorReason.lastIndexOf(",") == s3PaymentResponse.errorReason.lastIndex) {
                    s3PaymentResponse.errorReason = s3PaymentResponse.errorReason.substringBeforeLast(",")
                }
                paymentResponseList.add(s3PaymentResponse)
                errorCount ++
            } else {
                if (paymentObj != null) {
                    paymentList.add(paymentObj)
                }
            }
        }

        var s3Response: Any? = null
        if (errorCount > 0) {
            val file = writeIntos3PaymentResponseExcel(paymentResponseList, fileName)
            s3Response = s3Client.upload(s3Bucket, fileName, file!!)
        }

        var fileStats = Pair(recordCount, recordCount - errorCount)

        return Triple(paymentList, fileStats, s3Response.toString())
    }

    private fun getS3PaymentResponse(it: Map<String, Any>, errors: StringBuilder): BulkUploadErrorResponse {

        return BulkUploadErrorResponse(
            organizationSerialId = it["organization_serial_id"].toString(),
            tradePartySerialId = it["trade_party_serial_id"].toString(),
            entityCode = it["entity_code"].toString(),
            cogoAccountNo = it["cogo_account_no"].toString(),
            refAccountNo = it["ref_account_no"].toString(),
            currency = it["currency"].toString(),
            amount = it["amount"].toString(),
            exchangeRate = it["exchange_rate"].toString(),
            utr = it["utr"].toString(),
            payMode = it["pay_mode"].toString(),
            paymentDate = it["payment_date"].toString(),
            remarks = it["remarks"].toString(),
            errorReason = errors.toString()
        )
    }

    private fun writeIntos3PaymentResponseExcel(paymentResponseList: List<BulkUploadErrorResponse>, excelName: String): File? {
        val file = ExcelSheetBuilder.Builder()
            .filename(excelName)
            .sheetName("")
            .headerStyle( // Header style for all columns if you want to change the style of the individual header, you can pass style in the header object
                Style(
                    fontStyle = FontStyle.BOLD,
                    fontSize = 12,
                    fontColor = Color.BLACK,
                    background = Color.YELLOW
                )
            ).data(paymentResponseList).build()
        return file
    }

    private fun isPaymentPresentOnSage(paymentNumValue: String): Boolean {
        val paymentOnSage = "Select UMRNUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0 = '$paymentNumValue'"
        val resultForPaymentOnSageQuery = SageClient.sqlQuery(paymentOnSage)
        val responseMap = ObjectMapper().readValue<MutableMap<String, Any?>>(resultForPaymentOnSageQuery)
        val records = responseMap["recordset"] as? ArrayList<*>
        logger().info("Payment Present On Sage Response: $responseMap with size ${records?.size} ")
        return records?.size != 0
    }

    override suspend fun postPaymentToSage(paymentId: Long, performedBy: UUID): Boolean {
        try {
            val paymentDetails = paymentRepository.findByPaymentId(paymentId)
            if (paymentDetails.organizationId == AresConstants.BLUETIDE_OTPD_ID) {
                return false
            }

            if (paymentDetails.paymentDocumentStatus == PaymentDocumentStatus.POSTED) {
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        paymentDetails.paymentNumValue!!,
                        "Payment already posted",
                        false
                    )
                )
                return false
            }

            if (paymentDetails.paymentDocumentStatus == PaymentDocumentStatus.CREATED) {
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        paymentDetails.paymentNumValue!!,
                        "Payment is not approved",
                        false
                    )
                )
                return false
            }

            val isPaymentPresentOnSage = isPaymentPresentOnSage(paymentDetails.paymentNumValue!!)
            if (isPaymentPresentOnSage) {
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        paymentDetails.paymentNumValue!!,
                        "Payment number is already present on sage",
                        false
                    )
                )
                return false
            }

            val organization = railsClient.getListOrganizationTradePartyDetails(paymentDetails.organizationId!!)

            val sageOrganizationQuery = if (paymentDetails.accMode == AccMode.AR) "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'" else "Select BPSNUM_0 from $sageDatabase.BPSUPPLIER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'"
            val resultFromSageOrganizationQuery = SageClient.sqlQuery(sageOrganizationQuery)
            val recordsForSageOrganization = ObjectMapper().readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)
            if (recordsForSageOrganization.recordSet.isNullOrEmpty()) {
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        "Registration Number: ${organization.list[0]["registration_number"]}",
                        "Not Found BPR",
                        false
                    )
                )
                return false
            }
            val sageOrganizationFromSageId = if (paymentDetails.accMode == AccMode.AR) recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId else recordsForSageOrganization.recordSet?.get(0)?.sageSupplierId

            val sageOrganization = authClient.getSageOrganization(
                SageOrganizationRequest(
                    paymentDetails.orgSerialId.toString()
                )
            )

            if (sageOrganization.sageOrganizationId.isNullOrEmpty()) {
                paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        sageOrganization.toString(),
                        "Sage organization not present",
                        false
                    )
                )
                return false
            }

            if (sageOrganization.sageOrganizationId != sageOrganizationFromSageId) {
                paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        """Sage: $sageOrganizationFromSageId and Platform: ${sageOrganization.sageOrganizationId}""",
                        "sage serial organization id different on Sage and Cogoport Platform",
                        false
                    )
                )
                return false
            }

            lateinit var result: SageResponse
            val paymentLineItemDetails = getPaymentLineItem(paymentDetails)

            var bankCode: String? = null
            var entityCode: String? = null
            var currency: String? = null
            var bankCodeDetails = hashMapOf<String, String>()

            if (paymentDetails.paymentCode == PaymentCode.CTDS) {
                when (paymentDetails.entityCode) {
                    101 -> {
                        bankCode = "CTDS"
                        entityCode = paymentDetails.entityCode.toString()
                        currency = paymentDetails.currency
                    }
                    301 -> {
                        bankCode = "CTDSP"
                        entityCode = paymentDetails.entityCode.toString()
                        currency = paymentDetails.currency
                    }
                }
            } else {
                if (paymentDetails.payMode == PayMode.RAZORPAY) {
                    bankCode = PaymentSageGLCodes.RAZO.name
                    entityCode = paymentDetails.entityCode.toString()
                    currency = PaymentSageGLCodes.RAZO.currency
                } else {
                    bankCodeDetails = getPaymentGLCode(paymentDetails.cogoAccountNo!!)
                    bankCode = bankCodeDetails["bankCode"]!!
                    entityCode = bankCodeDetails["entityCode"].toString()
                    currency = bankCodeDetails["currency"]!!
                }

                if (paymentDetails.cogoAccountNo.isNullOrEmpty() && paymentDetails.payMode != PayMode.RAZORPAY) {
                    paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                    thirdPartyApiAuditService.createAudit(
                        ThirdPartyApiAudit(
                            null,
                            "PostPaymentToSage",
                            "Payment",
                            paymentId,
                            "PAYMENT",
                            "500",
                            sageOrganization.toString(),
                            "Cogo bank account number is null",
                            false
                        )
                    )
                    return false
                }
            }

            var jvSageAccount: String? = ""

            val bankDetails = CogoBankAccount.values().find { it.cogoAccountNo == paymentDetails.cogoAccountNo }
            if (!bankDetails?.cogoAccountNo.isNullOrEmpty()) {
                if (((paymentDetails.cogoAccountNo == bankDetails?.cogoAccountNo) && (paymentDetails.entityCode == bankCodeDetails["entityCode"]?.toInt()) && (paymentDetails.currency == bankCodeDetails["currency"])) || (paymentDetails.payMode == PayMode.RAZORPAY)) {
                    jvSageAccount = if (paymentDetails.accMode == AccMode.AP) JVSageAccount.AP.value else JVSageAccount.AR.value
                } else {
                    paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                    thirdPartyApiAuditService.createAudit(
                        ThirdPartyApiAudit(
                            null,
                            "PostPaymentToSage",
                            "Payment",
                            paymentId,
                            "PAYMENT",
                            "500",
                            sageOrganization.toString(),
                            "Bank Account details does not match",
                            false
                        )
                    )
                    return false
                }
            }

            result = SageClient.postPaymentToSage(
                PaymentRequest
                (
                    if (paymentDetails.accMode == AccMode.AP) PaymentCode.PAY.name else PaymentCode.REC.name,
                    paymentDetails.paymentNumValue!!,
                    sageOrganization.sageOrganizationId!!,
                    AresConstants.IND,
                    jvSageAccount!!,
                    bankCode,
                    paymentDetails.transactionDate!!,
                    currency!!,
                    entityCode!!,
                    if (paymentDetails.accMode == AccMode.AP) 1 else 2,
                    paymentDetails.amount.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.UP),
                    paymentDetails.transRefNumber,
                    paymentDetails.ledAmount!!.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.UP),
                    paymentLineItemDetails
                )
            )

            val processedResponse = XML.toJSONObject(result.response)
            val status = getStatus(processedResponse)

            if (status == 1) {
                val paymentNumOnSage = "Select NUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0 = '${paymentDetails.paymentNumValue!!}'"
                val resultForPaymentNumOnSageQuery = SageClient.sqlQuery(paymentNumOnSage)
                val mappedResponse = ObjectMapper().readValue<MutableMap<String, Any?>>(resultForPaymentNumOnSageQuery)
                val records = mappedResponse["recordset"] as? ArrayList<*>
                if (records?.size != 0) {
                    val queryResult = (records?.get(0) as LinkedHashMap<*, *>).get("NUM_0")
                    paymentRepository.updateSagePaymentNumValue(paymentId, queryResult.toString(), performedBy)
                } else {
                    paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                    thirdPartyApiAuditService.createAudit(
                        ThirdPartyApiAudit(
                            null,
                            "PostPaymentToSage",
                            "Payment",
                            paymentId,
                            "PAYMENT",
                            "404",
                            "UMRNUM_0: ${paymentDetails.paymentNumValue}",
                            "Sage Payment Num Value not present",
                            false
                        )
                    )
                    return false
                }
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "200",
                        result.requestString,
                        result.response,
                        true
                    )
                )
                return true
            } else {
                paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        result.requestString,
                        result.response,
                        false
                    )
                )
            }
        } catch (exception: SageException) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostPaymentToSage",
                    "Payment",
                    paymentId,
                    "PAYMENT",
                    "500",
                    exception.data.trim(),
                    exception.context,
                    false
                )
            )
        } catch (aresException: AresException) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostPaymentToSage",
                    "Payment",
                    paymentId,
                    "PAYMENT",
                    "500",
                    paymentId.toString(),
                    "${aresException.error.message} ${aresException.context}",
                    false
                )
            )
        } catch (e: Exception) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostPaymentToSage",
                    "Payment",
                    paymentId,
                    "PAYMENT",
                    "500",
                    paymentId.toString(),
                    e.toString(),
                    false
                )
            )
        }
        return false
    }

    override suspend fun bulkPostPaymentToSage(paymentIds: List<Long>, performedBy: UUID) {
        val statusGrouping = paymentRepository.getPaymentDocumentStatusWiseIds(paymentIds)
        statusGrouping?.map {
            when (it.paymentDocumentStatus) {
                PaymentDocumentStatus.CREATED -> {
                    it.paymentIds.map { paymentId ->
                        aresMessagePublisher.emitBulkUpdatePaymentAndPostOnSage(PostPaymentToSage(paymentId, performedBy))
                    }
                }
                PaymentDocumentStatus.APPROVED, PaymentDocumentStatus.POSTING_FAILED -> {
                    it.paymentIds.map { paymentId ->
                        aresMessagePublisher.emitBulkPostPaymentToSage(PostPaymentToSage(paymentId, performedBy))
                    }
                }
                PaymentDocumentStatus.POSTED -> {
                    it.paymentIds.map { paymentId ->
                        aresMessagePublisher.emitBulkPostPaymentFromSage(PostPaymentToSage(paymentId, performedBy))
                    }
                }
                else -> return
            }
        }
    }

    private fun getPaymentLineItem(payment: com.cogoport.ares.api.payment.entity.Payment): PaymentLineItem {
        return PaymentLineItem(
            accMode = if (payment.accMode == AccMode.AP) JVSageControls.AP.value else JVSageControls.AR.value,
        )
    }

    private fun getPaymentGLCode(cogoAccountNo: String): HashMap<String, String> {
        val bankCode = CogoBankAccount.values().find { it.cogoAccountNo == cogoAccountNo }?.name ?: throw AresException(AresError.ERR_1538, "")

        val currency = PaymentSageGLCodes.valueOf(bankCode).currency
        val entityCode = PaymentSageGLCodes.valueOf(bankCode).entityCode
        return hashMapOf(
            "bankCode" to bankCode,
            "currency" to currency,
            "entityCode" to entityCode.toString()
        )
    }

    private fun getStatus(processedResponse: JSONObject?): Int? {
        val status = processedResponse?.getJSONObject("soapenv:Envelope")
            ?.getJSONObject("soapenv:Body")
            ?.getJSONObject("wss:runResponse")
            ?.getJSONObject("runReturn")
            ?.getJSONObject("status")
            ?.get("content")
        return status as Int?
    }

    override suspend fun postPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse {
        val failedIds: MutableList<Long?> = mutableListOf()
        for (id in paymentIds) {
            try {
                val payment = paymentRepository.findByPaymentId(id)
                if (!isPaymentPresentOnSage(payment.paymentNumValue!!)) {
                    logger().info("Document ${payment.paymentNumValue} is not present on sage for final posting")
                    createThirdPartyAudit(id, "PostPaymentFromSage", payment.paymentNumValue.toString(), "Document is not present on sage for final posting", false)
                    failedIds.add(id)
                    return SageFailedResponse(
                        failedIdsList = failedIds
                    )
                }

                val result = SageClient.postPaymentFromSage(payment.sageRefNumber!!)
                val processedResponse = XML.toJSONObject(result.response)
                val status = getStatus(processedResponse)
                if (status == 1) {
                    createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, result.response, true)
                    paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.FINAL_POSTED, performedBy)
                } else {
                    createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, result.response, false)
                    failedIds.add(id)
                }
            } catch (sageException: SageException) {
                createThirdPartyAudit(id, "PostPaymentFromSage", sageException.data, sageException.context, false)
                failedIds.add(id)
            } catch (e: Exception) {
                createThirdPartyAudit(id, "PostPaymentFromSage", id.toString(), e.toString(), false)
                failedIds.add(id)
            }
        }
        return SageFailedResponse(
            failedIdsList = failedIds
        )
    }

    override suspend fun cancelPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse {
        val failedIds: MutableList<Long?> = mutableListOf()
        for (id in paymentIds) {
            try {
                val payment = paymentRepository.findByPaymentId(id)
                if (sageServiceImpl.isPaymentPostedFromSage(payment.paymentNumValue!!) == null) {
                    throw AresException(AresError.ERR_1536, "")
                }
                val result = SageClient.cancelPaymentFromSage(payment.sageRefNumber!!)
                val processedResponse = XML.toJSONObject(result.response)
                val status = getStatus(processedResponse)
                if (status == 1) {
                    createThirdPartyAudit(id, "CancelPaymentFromSage", result.requestString, result.response, true)
                    paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.POSTED, performedBy)
                } else {
                    createThirdPartyAudit(id, "CancelPaymentFromSage", result.requestString, result.response, false)
                    failedIds.add(id)
                }
            } catch (sageException: SageException) {
                createThirdPartyAudit(id, "CancelPaymentFromSage", sageException.data, sageException.context, false)
                failedIds.add(id)
            } catch (aresException: AresException) {
                createThirdPartyAudit(id, "CancelPaymentFromSage", id.toString(), "${aresException.error.message} ${aresException.context}", false)
                failedIds.add(id)
            } catch (e: Exception) {
                createThirdPartyAudit(id, "CancelPaymentFromSage", id.toString(), e.toString(), false)
                failedIds.add(id)
            }
        }
        return SageFailedResponse(
            failedIdsList = failedIds
        )
    }

    private suspend fun createThirdPartyAudit(id: Long, apiName: String, request: String, response: String, isSuccess: Boolean) {
        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                null,
                apiName,
                "Payment",
                id,
                "PAYMENT",
                if (isSuccess) "200" else "500",
                request,
                response,
                isSuccess
            )
        )
    }

    override suspend fun createPaymentEntryAndReturnUtr(request: Payment) {
        val response: OnAccountApiCommonResponse
        try {
            response = createPaymentEntry(request)
            val onAccountWithUtrResponse = OnAccountWithUtrResponse(
                paymentId = response.id,
                message = response.message,
                isSuccess = response.isSuccess,
                transactionRefNo = request.utr!!
            )
            kuberMessagePublisher.updateAdvanceDocumentStatus(onAccountWithUtrResponse)
        } catch (e: Exception) {
            val onAccountWithUtrResponse = OnAccountWithUtrResponse(
                paymentId = null,
                message = null,
                isSuccess = false,
                transactionRefNo = request.utr!!
            )
            kuberMessagePublisher.updateAdvanceDocumentStatus(onAccountWithUtrResponse)
        }
    }

    override suspend fun directFinalPostToSage(req: PostPaymentToSage) {
        val postingStatusData = postPaymentToSage(req.paymentId, req.performedBy)

        if (postingStatusData) {
            postPaymentFromSage(arrayListOf(req.paymentId), req.performedBy)
        }
    }

    override suspend fun bulkUpdatePaymentAndPostOnSage(req: PostPaymentToSage) {
        val payment = paymentRepository.findByPaymentId(req.paymentId)
        val paymentModel = paymentConverter.convertToModel(payment)
        paymentModel.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
        paymentModel.updatedBy = req.performedBy.toString()
        val updatedPayment = updatePaymentEntry(paymentModel)
        if (updatedPayment.isSuccess && paymentModel.organizationId != AresConstants.BLUETIDE_OTPD_ID) {
            directFinalPostToSage(
                PostPaymentToSage(
                    req.paymentId,
                    req.performedBy
                )
            )
        }
    }

    override suspend fun downloadSagePlatformReport(startDate: String, endDate: String) {
        val platformPaymentData = unifiedDBRepo.getPaymentsByTransactionDate(
            startDate,
            endDate
        )
        val paymentNumValues = platformPaymentData
            ?.map { it.paymentNumValue }
            ?.toCollection(ArrayList())

        val platformAndSagePaymentResponse = mutableListOf<SagePlatformPaymentHeader>()

        if (!paymentNumValues.isNullOrEmpty()) {
            val sagePaymentData = sageServiceImpl.sagePaymentBySageRefNumbers(paymentNumValues)
            platformPaymentData.map { platformPayment ->
                val sagePayment = sagePaymentData.firstOrNull { it.platformPaymentNum == platformPayment.paymentNumValue }

                platformAndSagePaymentResponse.add(
                    SagePlatformPaymentHeader(
                        paymentNumValueAtPlatform = platformPayment.paymentNumValue,
                        sageOrganizationIdAtPlatform = platformPayment.sageOrganizationId,
                        paymentCodeAtPlatform = platformPayment.paymentCode,
                        bankCodeAtPlatform = platformPayment.accCode,
                        entityCodeAtPlatform = platformPayment.entityCode,
                        currencyAtPlatform = platformPayment.currency,
                        amountAtPlatform = platformPayment.amount,
                        utrAtPlatform = platformPayment.utr,
                        paymentNumValueAtSage = sagePayment?.sagePaymentNum,
                        sageOrganizationIdAtSage = sagePayment?.bprNumber,
                        paymentCodeAtSage = sagePayment?.paymentCode,
                        bankCodeAtSage = sagePayment?.glCode,
                        entityCodeAtSage = sagePayment?.entityCode,
                        currencyAtSage = sagePayment?.currency,
                        amountAtSage = sagePayment?.amount,
                        utrAtSage = sagePayment?.narration,
                        isAmountMatched = sagePayment?.amount?.compareTo(platformPayment.amount) == 0,
                        isCurrencyMatched = sagePayment?.currency == platformPayment.currency,
                        isBPRMatched = sagePayment?.sageOrganizationId == platformPayment.sageOrganizationId,
                        isEntityMatched = sagePayment?.entityCode == platformPayment.entityCode,
                        isPanMatched = sagePayment?.panNumber == platformPayment.panNumber,
                        panNumberAtPlatform = platformPayment.panNumber,
                        panNumberAtSage = sagePayment?.panNumber,
                        isGLCodeMatched = sagePayment?.glCode?.compareTo(platformPayment.accCode!!) == 0,
                        isUtrMatched = sagePayment?.narration == platformPayment.utr
                    )
                )
            }
            val excelName: String = "Payment_Report" + "_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss")) + ".xlsx"

            val file = ExcelUtils.writeIntoExcel(platformAndSagePaymentResponse, excelName, "Payment_Report")
            val url = s3Client.upload(s3Bucket, excelName, file).toString()
            val excelDocument = aresDocumentRepository.save(
                AresDocument(
                    id = null,
                    documentUrl = url,
                    documentName = "sage_platform_report",
                    documentType = url.substringAfterLast('.'),
                    uploadedBy = AresConstants.ARES_USER_ID,
                    createdAt = Timestamp.valueOf(LocalDateTime.now()),
                    updatedAt = Timestamp.valueOf(LocalDateTime.now())
                )
            )

            val visibleUrl = "$baseUrl/payments/download?id=${Hashids.encode(excelDocument.id!!)}"

            val emailVariables: HashMap<String, String?> = hashMapOf("file_url" to visibleUrl, "type" to "Payment", "subject" to "Sage Platform Payment Report")

            val request = CreateCommunicationRequest(
                templateName = AresConstants.SAGE_PLATFORM_REPORT,
                performedByUserId = AresConstants.ARES_USER_ID,
                performedByUserName = AresConstants.performedByUserNameForMail,
                recipientEmail = AresConstants.RECIPIENT_EMAIL_FOR_EVERYDAY_SAGE_PLATFORM_REPORT,
                senderEmail = AresConstants.NO_REPLY,
                ccEmails = AresConstants.CC_MAIL_ID_FOR_EVERYDAY_SAGE_PLATFORM_REPORT,
                emailVariables = emailVariables,
            )

            aresMessagePublisher.sendEmail(request)
        }
    }

    override suspend fun deletingApPayments(paymentNumValues: List<String>) {
        val paymentsData = paymentRepository.getPaymentRelatedField(AccMode.AP.name, paymentNumValues)

        if (paymentsData.isNotEmpty()) {
            val docValues = paymentsData.map { it.paymentNumValue!! }

            // deleting payments
            paymentRepository.deletingApPayments(docValues)

            // marking account utilization deleted
            accountUtilizationRepository.updateAccountUtilizationUsingDocValue(docValues)

            // marking settlement deleted for utilized payments
            settlementRepository.markingSettlementAsDeleted(paymentsData.map { it.paymentNum!! }, paymentsData.map { it.paymentCode.toString() })
        }
    }

    override suspend fun getARLedgerOrganizationAndEntityWise(req: LedgerSummaryRequest): List<ARLedgerResponse> {
        val ledgerSelectedDateWise = accountUtilizationRepository.getARLedger(
            AccMode.AR,
            req.orgId,
            req.entityCodes!!,
            req.startDate!!,
            req.endDate!!
        )
        val previousLedger = accountUtilizationRepository.getOpeningAndClosingLedger(AccMode.AR, req.orgId, req.entityCodes!!, req.startDate!!, AresConstants.OPENING_BALANCE)
        val currentLedger = accountUtilizationRepository.getOpeningAndClosingLedger(AccMode.AR, req.orgId, req.entityCodes!!, req.endDate, AresConstants.CLOSING_BALANCE)
        return previousLedger + ledgerSelectedDateWise + currentLedger
    }
}
