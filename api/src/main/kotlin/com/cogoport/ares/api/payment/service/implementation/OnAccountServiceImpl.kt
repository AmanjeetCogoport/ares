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
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdsInput
import com.cogoport.ares.api.migration.service.interfaces.SageService
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
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.sage.service.implementation.SageServiceImpl
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
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
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.BulkUploadErrorResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.PaymentResponse
import com.cogoport.ares.model.payment.response.PlatformOrganizationResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.sage.SageFailedResponse
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
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil
import com.cogoport.brahma.sage.Client as SageClient

@Singleton
open class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

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
    lateinit var settlementService: SettlementService

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var cogoBackLowLevelClient: CogoBackLowLevelClient

    @Inject
    lateinit var sageServiceImpl: SageServiceImpl

    @Inject lateinit var sageService: SageService

    @Inject
    lateinit var openSearchService: OpenSearchService

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse {
        val total: Int
        val payments: List<PaymentResponse?>?
        var startDate: Timestamp? = null
        var endDate: Timestamp? = null
        if (request.startDate != null && request.endDate != null) {
            startDate = Timestamp.valueOf(request.startDate)
            endDate = Timestamp.valueOf(request.endDate)
        }
        val data = OpenSearchClient().onAccountSearch(request, PaymentResponse::class.java)!!
        payments = data.hits().hits().map { it.source() }
        payments.map {
            it?.paymentDocumentStatus = paymentRepository.getPaymentDocumentStatus(it?.id!!)
        }
        total = data.hits().total().value().toInt()
        return AccountCollectionResponse(list = payments, totalRecords = total, totalPage = ceil(total.toDouble() / request.pageLimit.toDouble()).toInt(), page = request.page)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        val filterDateFromTs = Timestamp(dateFormat.parse(receivableRequest.paymentDate).time)
        receivableRequest.transactionDate = filterDateFromTs

        setPaymentAmounts(receivableRequest)
        val paymentId = createNonSuspensePaymentEntry(receivableRequest)

        return OnAccountApiCommonResponse(id = paymentId, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    private suspend fun createNonSuspensePaymentEntry(receivableRequest: Payment): Long {
        if (receivableRequest.accMode == null) receivableRequest.accMode = AccMode.AR

        var isUtrExit: Boolean? = false
        if (receivableRequest.accMode == AccMode.AR) {
            isUtrExit = paymentRepository.isARTransRefNumberExists(accMode = AccMode.AR.name, transRefNumber = receivableRequest.utr!!)
        }

        if (isUtrExit == true) {
            throw AresException(AresError.ERR_1537, "")
        }
        receivableRequest.signFlag = when (receivableRequest.docType == "TDS") {
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

        when (receivableRequest.isPosted ?: false) {
            true -> {
                receivableRequest.isPosted = true
                receivableRequest.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
            }
            false -> {
                receivableRequest.isPosted = false
                receivableRequest.paymentDocumentStatus = PaymentDocumentStatus.CREATED
            }
        }

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

        if (receivableRequest.isPosted != true) {
            receivableRequest.isPosted = false
        }
        receivableRequest.serviceType = ServiceType.NA
        receivableRequest.isDeleted = false
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

        accUtilEntity.accCode = when (receivableRequest.docType == "TDS") {
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

        // need to verify
        if (receivableRequest.docType == "TDS") {
            accUtilEntity.isVoid = false
            accUtilEntity.tdsAmountLoc = BigDecimal.ZERO
            accUtilEntity.tdsAmount = BigDecimal.ZERO
        }

        val accUtilRes = accountUtilizationRepository.save(accUtilEntity)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accUtilEntity.organizationId))

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
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest, true)

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            if (accUtilRes.accMode == AccMode.AP) {
                aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilRes.organizationId))
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

        val accType = receivableRequest.paymentCode?.name ?: throw AresException(AresError.ERR_1003, "paymentCode")
        val payment = receivableRequest.id?.let { paymentRepository.findByPaymentId(it) } ?: throw AresException(AresError.ERR_1002, "")

        if (payment.isPosted) throw AresException(AresError.ERR_1010, "")
        val accountUtilization = accountUtilizationRepository.findRecord(payment.paymentNum!!, accType, accMode) ?: throw AresException(AresError.ERR_1002, "")
        return updateNonSuspensePayment(receivableRequest, accountUtilization, payment)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updateNonSuspensePayment(receivableRequest: Payment, accountUtilizationEntity: AccountUtilization, paymentEntity: com.cogoport.ares.api.payment.entity.Payment): OnAccountApiCommonResponse {

        if (receivableRequest.isPosted != null && receivableRequest.isPosted == true) {
            paymentEntity.isPosted = true
            accountUtilizationEntity.documentStatus = DocumentStatus.FINAL
            paymentEntity.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
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
        val openSearchPaymentModel = paymentConverter.convertToModel(paymentDetails)
        openSearchPaymentModel.paymentDate = paymentDetails.transactionDate?.toLocalDate().toString()
        openSearchPaymentModel.uploadedBy = receivableRequest.uploadedBy

        /*UPDATE THE DATABASE WITH UPDATED ACCOUNT UTILIZATION ENTRY*/
        val accUtilRes = accountUtilizationRepository.update(accountUtilizationEntity)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilizationEntity.organizationId))

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
        /*UPDATE THE OPEN SEARCH WITH UPDATED PAYMENT ENTRY*/
        Client.addDocument(
            AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentDetails.id.toString(), openSearchPaymentModel,
            true
        )

        try {
            /*UPDATE THE OPEN SEARCH WITH UPDATED ACCOUNT UTILIZATION ENTRY */
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            // EMITTING KAFKA MESSAGE TO UPDATE OUTSTANDING and DASHBOARD
            emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }
        return OnAccountApiCommonResponse(id = accUtilRes.id!!, message = Messages.PAYMENT_UPDATED, isSuccess = true)
    }

    private fun updatePaymentEntity(receivableRequest: Payment, paymentEntity: com.cogoport.ares.api.payment.entity.Payment) {
        val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        val paymentDate = (receivableRequest.paymentDate ?: paymentEntity.transactionDate).toString()
        val filterDateFromTs = Timestamp(dateFormat.parse(paymentDate).time)
        paymentEntity.entityCode = receivableRequest.entityType ?: paymentEntity.entityCode
        paymentEntity.bankName = receivableRequest.bankName ?: paymentEntity.bankName
        paymentEntity.payMode = receivableRequest.payMode ?: paymentEntity.payMode
        paymentEntity.transactionDate = filterDateFromTs
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
        val payment = paymentRepository.findByPaymentId(deletePaymentRequest.paymentId) ?: throw AresException(AresError.ERR_1001, "")
        if (payment.isDeleted)
            throw AresException(AresError.ERR_1007, "")

        payment.isDeleted = true
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
        val openSearchPaymentModel = paymentConverter.convertToModel(paymentResponse)
        openSearchPaymentModel.paymentDate = paymentResponse.transactionDate?.toLocalDate().toString()

        val accType = AccountType.valueOf(payment.paymentCode?.name!!)

        val accountUtilization = accountUtilizationRepository.findRecord(
            payment.paymentNum!!, accType.name, deletePaymentRequest.accMode?.name
        ) ?: throw AresException(AresError.ERR_1005, "")
        accountUtilization.documentStatus = DocumentStatus.DELETED

        /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN DATABASE*/
        val accUtilRes = accountUtilizationRepository.update(accountUtilization)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilization.organizationId))

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
        /*MARK THE PAYMENT AS DELETED IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, payment.id.toString(), openSearchPaymentModel, true)

        if (payment.isPosted) {
            val request = DeleteSettlementRequest(
                documentNo = Hashids.encode(payment.paymentNum!!),
                deletedBy = UUID.fromString(deletePaymentRequest.performedById),
                deletedByUserType = deletePaymentRequest.performedByUserType,
                settlementType = SettlementType.PAY
            )
            settlementService.delete(request)
        }
        try {
            /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN OPEN SEARCH*/
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes, true)
//             Emitting Kafka message to Update Outstanding and Dashboard
            emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
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

    private suspend fun setPaymentEntity(payment: com.cogoport.ares.api.payment.entity.Payment, docType: String?) {
        val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
        when (docType == "TDS") {
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
        payment.isDeleted = false
    }

    private fun setAccountUtilizationModel(accUtilizationModel: AccUtilizationRequest, receivableRequest: Payment) {
        accUtilizationModel.accType = when (receivableRequest.docType == "TDS") {
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
        accUtilizationModel.docStatus = when (receivableRequest.isPosted!!) {
            true -> DocumentStatus.FINAL
            false -> DocumentStatus.PROFORMA
        }
        accUtilizationModel.migrated = false
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

    override suspend fun getOrgStatsForCoeFinance(orgId: UUID?): OrgStatsResponseForCoeFinance {
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
            var res = createBulkPayments(paymentModelList)
            recordsInserted = res?.recordsInserted
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
                tradePartyDetailSerialId = it["trade_party_serial_id"].toString().toLong()
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
            var errors = StringBuilder()
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
                detail?.organization?.orgSerialId == organizationSerialNo.toLong() && detail?.tradePartySerial.toString() == tradePartySerialNo
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

            var paymentObj = Payment(
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
                uploadedBy = uploadedByName?.get(0)?.userName,
                tradePartyMappingId = serialIdDetails?.mappingId,
                taggedOrganizationId = serialIdDetails?.organizationId,
                paymentDocumentStatus = PaymentDocumentStatus.CREATED,
                docType = "PAYMENT"
            )

            if (hasErrors) {
                var s3PaymentResponse = getS3PaymentResponse(it, errors)
                if (s3PaymentResponse.errorReason?.lastIndexOf(",") == s3PaymentResponse.errorReason?.lastIndex) {
                    s3PaymentResponse.errorReason = s3PaymentResponse.errorReason?.substringBeforeLast(",")
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
            val paymentDetails = paymentRepository.findByPaymentId(paymentId) ?: throw AresException(AresError.ERR_1002, "")

            val uploadedByName = authClient.getUsers(
                GetUserRequest(
                    id = arrayListOf(paymentDetails.createdBy.toString())
                )
            )

            val openSearchPaymentModel = paymentConverter.convertToModel(paymentDetails)
            openSearchPaymentModel.updatedBy = performedBy.toString()
            openSearchPaymentModel.uploadedBy = if (uploadedByName?.size != 0) {
                uploadedByName?.get(0)?.userName
            } else {
                ""
            }
            openSearchPaymentModel.paymentDate = paymentDetails.transactionDate?.toLocalDate().toString()

            if (paymentDetails.paymentDocumentStatus == PaymentDocumentStatus.POSTED) {
                throw AresException(AresError.ERR_1523, "")
            }

            if (paymentDetails.paymentDocumentStatus == PaymentDocumentStatus.CREATED) {
                throw AresException(AresError.ERR_1524, "")
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
                throw AresException(AresError.ERR_1002, "BPR")
            }
            val sageOrganizationFromSageId = if (paymentDetails.accMode == AccMode.AR) recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId else recordsForSageOrganization.recordSet?.get(0)?.sageSupplierId

            val sageOrganization = authClient.getSageOrganization(
                SageOrganizationRequest(
                    paymentDetails.orgSerialId.toString(),
                    if (paymentDetails.accMode == AccMode.AP) {
                        "service_provider"
                    } else {
                        "importer_exporter"
                    }
                )
            )

            if (sageOrganization.sageOrganizationId.isNullOrEmpty()) {
                paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTING_FAILED, performedBy)
                openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTING_FAILED
                Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)
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
                openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTING_FAILED
                Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostPaymentToSage",
                        "Payment",
                        paymentId,
                        "PAYMENT",
                        "500",
                        sageOrganization.toString(),
                        "sage serial organization id different in sage db and cogoport db",
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
                    openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTING_FAILED
                    Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)
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
                    openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTING_FAILED
                    Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)
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
                    paymentDetails.amount,
                    paymentDetails.transRefNumber,
                    paymentLineItemDetails
                )
            )

            val processedResponse = XML.toJSONObject(result.response)
            val status = getStatus(processedResponse)

            if (status == 1) {
                paymentRepository.updatePaymentDocumentStatus(paymentId, PaymentDocumentStatus.POSTED, performedBy)
                openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTED
                Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)

                val paymentNumOnSage = "Select NUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0 = '${paymentDetails.paymentNumValue!!}'"
                val resultForPaymentNumOnSageQuery = SageClient.sqlQuery(paymentNumOnSage)
                val mappedResponse = ObjectMapper().readValue<MutableMap<String, Any?>>(resultForPaymentNumOnSageQuery)
                val records = mappedResponse["recordset"] as? ArrayList<*>

                if (records?.size != 0) {
                    val queryResult = (records?.get(0) as LinkedHashMap<*, *>).get("NUM_0")
                    paymentRepository.updateSagePaymentNumValue(paymentId, queryResult.toString())
                } else {
                    thirdPartyApiAuditService.createAudit(
                        ThirdPartyApiAudit(
                            null,
                            "PostPaymentToSage",
                            "Payment",
                            paymentId,
                            "PAYMENT",
                            "500",
                            records.toString(),
                            "Sage Payment Num Value not present",
                            false
                        )
                    )
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
                openSearchPaymentModel.paymentDocumentStatus = PaymentDocumentStatus.POSTING_FAILED
                Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentId.toString(), openSearchPaymentModel, true)
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
            throw exception
        } catch (e: Exception) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostPaymentToSage",
                    "Payment",
                    paymentId,
                    "PAYMENT",
                    "500",
                    "",
                    e.toString(),
                    false
                )
            )
            throw e
        }
        return false
    }

    private fun getPaymentLineItem(payment: com.cogoport.ares.api.payment.entity.Payment): PaymentLineItem {
        return PaymentLineItem(
            accMode = if (payment.accMode == AccMode.AP) JVSageControls.AP.value else JVSageControls.AR.value,
        )
    }

    private fun getPaymentGLCode(cogoAccountNo: String): HashMap<String, String> {
        val bankCode = CogoBankAccount.values().find { it.cogoAccountNo == cogoAccountNo }?.name
        val currency = PaymentSageGLCodes.valueOf(bankCode!!).currency
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
                val paymentFromOpenSearch = openSearchService.fetchPaymentFromOpenSearch(id)

                if (payment?.paymentDocumentStatus != PaymentDocumentStatus.POSTED) {
                    throw AresException(AresError.ERR_1535, "")
                }
                if (!isPaymentPresentOnSage(payment.paymentNumValue!!)) {
                    throw AresException(AresError.ERR_1002, "payment ${payment.paymentNumValue} is not present on sage for final posting")
                }

                val result = SageClient.postPaymentFromSage(payment.sageRefNumber!!)
                val processedResponse = XML.toJSONObject(result.response)
                val status = getStatus(processedResponse)
                if (status == 1) {
                    createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, result.response, true)
                    paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.FINAL_POSTED, performedBy)
                    paymentFromOpenSearch.paymentDocumentStatus = PaymentDocumentStatus.FINAL_POSTED
                    Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, id.toString(), paymentFromOpenSearch, true)
                } else {
                    createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, result.response, false)
                    failedIds.add(id)
                }
            } catch (e: Exception) {
                createThirdPartyAudit(id, "PostPaymentFromSage", "", e.toString(), false)
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
                val paymentFromOpenSearch = openSearchService.fetchPaymentFromOpenSearch(id)

                if (sageServiceImpl.isPaymentPostedFromSage(payment.paymentNumValue!!) == null) {
                    throw AresException(AresError.ERR_1536, "")
                }
                val result = SageClient.cancelPaymentFromSage(payment.sageRefNumber!!)
                val processedResponse = XML.toJSONObject(result.response)
                val status = getStatus(processedResponse)
                if (status == 1) {
                    createThirdPartyAudit(id, "CancelPaymentFromSage", result.requestString, result.response, true)
                    paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.POSTED, performedBy)
                    paymentFromOpenSearch.paymentDocumentStatus = PaymentDocumentStatus.POSTED
                    Client.updateDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, id.toString(), paymentFromOpenSearch, true)
                } else {
                    createThirdPartyAudit(id, "CancelPaymentFromSage", result.requestString, result.response, false)
                    failedIds.add(id)
                }
            } catch (e: Exception) {
                createThirdPartyAudit(id, "CancelPaymentFromSage", "", e.toString(), false)
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
}
