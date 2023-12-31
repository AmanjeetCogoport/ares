package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.Validations
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.IncidentStatus
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.common.models.BankDetails
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.events.PlutusMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresError.ERR_1002
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.migration.model.SagePlatformPaymentHeader
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdsInput
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.AresDocument
import com.cogoport.ares.api.payment.entity.OrgIdAndEntityCode
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
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentFileRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.sage.service.implementation.SageServiceImpl
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.repository.GlCodeMasterRepository
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.implementation.SettlementServiceHelper
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
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
import com.cogoport.ares.model.payment.AccMode.AP
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountType.EXP
import com.cogoport.ares.model.payment.AccountType.PAY
import com.cogoport.ares.model.payment.AccountType.VTDS
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
import com.cogoport.ares.model.payment.ServiceType.EXPENSE
import com.cogoport.ares.model.payment.TradePartyDetailRequest
import com.cogoport.ares.model.payment.TradePartyOrganizationResponse
import com.cogoport.ares.model.payment.UpdateCSDPaymentRequest
import com.cogoport.ares.model.payment.ValidateTradePartyRequest
import com.cogoport.ares.model.payment.enum.CogoBankAccount
import com.cogoport.ares.model.payment.enum.PaymentSageGLCodes
import com.cogoport.ares.model.payment.request.ARLedgerRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.request.SaasInvoiceHookRequest
import com.cogoport.ares.model.payment.request.UpdateOrganizationDetailAresSideRequest
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
import com.cogoport.ares.model.payment.response.SaasInvoiceHookResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.ares.model.sage.SageOrganizationAccountTypeRequest
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SettlementType.PINV
import com.cogoport.ares.model.settlement.enums.JVSageAccount
import com.cogoport.ares.model.settlement.enums.JVSageControls
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.JvLineItemRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
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
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentSubTypeEnum
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.enums.Source
import com.cogoport.hades.model.incident.request.AdvanceSecurityDepositRefund
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import com.cogoport.plutus.client.PlutusClient
import com.cogoport.plutus.model.invoice.GetUserRequest
import com.cogoport.plutus.model.invoice.InvoiceStatusUpdateRequest
import com.cogoport.plutus.model.invoice.enums.InvoiceStatus
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
    lateinit var plutusMessagePublisher: PlutusMessagePublisher

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

    @Inject
    lateinit var parentJVService: ParentJVService

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    @Inject
    lateinit var util: Util

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    @Inject
    lateinit var unifiedDBNewRepository: UnifiedDBNewRepository

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    lateinit var plutusClient: PlutusClient

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var glCodeMasterRepository: GlCodeMasterRepository

    @Inject
    lateinit var invoicePayMappingRepository: InvoicePayMappingRepository

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
        val paymentId = if (receivableRequest.advanceDocumentId != null) {
            createNonSuspensePaymentEntryCSDWrapper(receivableRequest)
        } else {
            createNonSuspensePaymentEntry(receivableRequest)
        }

        return OnAccountApiCommonResponse(id = paymentId, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    private suspend fun createNonSuspensePaymentEntryCSDWrapper(receivableRequest: Payment): Long {
        receivableRequest.accCode = AresModelConstants.CSD_ACCOUNT_CODE
        receivableRequest.paymentCode = PaymentCode.REC
        val savedPaymentId = createNonSuspensePaymentEntry(receivableRequest)

        if (receivableRequest.advanceDocumentId != null) {
            hadesClient.createIncident(
                CreateIncidentRequest(
                    type = IncidentType.ADVANCE_SECURITY_DEPOSIT_REFUND,
                    description = null,
                    data = IncidentData(
                        advanceSecurityDepositRefund = AdvanceSecurityDepositRefund(
                            paymentId = savedPaymentId,
                            advanceDocumentId = receivableRequest.advanceDocumentId!!,
                            utrNumber = receivableRequest.utr,
                            currency = receivableRequest.currency,
                            totalAmount = receivableRequest.amount,
                            remark = receivableRequest.remarks,
                            shipmentId = null,
                            supplierName = receivableRequest.serviceProvider,
                            uploadProof = null,
                            sid = receivableRequest.jobNumber,
                            paymentDocUrl = receivableRequest.paymentDocUrl
                        ),
                        organization = Organization(
                            id = receivableRequest.organizationId,
                            businessName = receivableRequest.organizationName,
                            tradePartyName = receivableRequest.organizationName
                        )
                    ),
                    source = Source.SHIPMENT,
                    createdBy = UUID.fromString(receivableRequest.createdBy),
                    entityId = UUID.fromString(AresConstants.ENTITY_ID[receivableRequest.entityType]),
                    incidentSubType = IncidentSubTypeEnum.ADVANCE_SECURITY_DEPOSIT_REFUND
                )
            )
        }
        return savedPaymentId
    }

    private suspend fun createNonSuspensePaymentEntry(receivableRequest: Payment): Long {
        validatingCreatePaymentRequest(receivableRequest)

        receivableRequest.paymentCode = receivableRequest.paymentCode ?: when (receivableRequest.docType == DocType.TDS) {
            true -> when (receivableRequest.accMode) {
                AccMode.AR -> PaymentCode.CTDS
                AccMode.AP -> PaymentCode.VTDS
                else -> throw AresException(AresError.ERR_1553, "")
            }
            else -> when (receivableRequest.accMode) {
                AccMode.AR -> PaymentCode.REC
                AccMode.AP -> PaymentCode.PAY
                else -> throw AresException(AresError.ERR_1553, "")
            }
        }

//        setOrganizations(receivableRequest)
//        setTradePartyOrganizations(receivableRequest)
        setTradePartyInfo(receivableRequest, null)

        val payment = paymentConverter.convertToEntity(receivableRequest)

        setPaymentEntity(payment)

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
        accUtilEntity.accCode = savedPayment.accCode
        accUtilEntity.isVoid = false

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
                aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accUtilRes.organizationId!!, accUtilRes.entityCode))
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
        val payment = validatingUpdatePaymentRequest(receivableRequest)
        val accType = receivableRequest.paymentCode?.name!!
        val accMode = receivableRequest.accMode?.name
        val accountUtilization = accountUtilizationRepository.findRecord(documentNo = payment.paymentNum!!, accType = accType, accMode = accMode) ?: throw AresException(AresError.ERR_1002, "Account Utilization")
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
                aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountUtilizationEntity.organizationId!!, accountUtilizationEntity.entityCode))
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
            paymentRepository.update(payment)
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
                    aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountUtilization.organizationId!!, accountUtilization.entityCode))
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

    private suspend fun setPaymentEntity(payment: com.cogoport.ares.api.payment.entity.Payment) {
        val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
        payment.accCode = AresModelConstants.ACC_MODE_PAYMENT_CODE_MAPPING["${payment.accMode.name}_${payment.paymentCode?.name}"]!!
        val sequence = when (payment.paymentCode) {
            PaymentCode.PAY -> SequenceSuffix.PAYMENT.prefix
            PaymentCode.REC -> SequenceSuffix.RECEIVED.prefix
            else -> SequenceSuffix.valueOf(payment.paymentCode?.name!!).prefix
        }
        payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(sequence)
        payment.paymentNumValue = payment.paymentCode.toString() + financialYearSuffix + payment.paymentNum
        payment.signFlag = SignSuffix.valueOf(payment.paymentCode?.name!!).sign
        payment.migrated = false
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())
        payment.paymentDocumentStatus = payment.paymentDocumentStatus ?: PaymentDocumentStatus.CREATED
    }

    private fun setAccountUtilizationModel(accUtilizationModel: AccUtilizationRequest, receivableRequest: Payment) {
        accUtilizationModel.accType = AccountType.valueOf(receivableRequest.paymentCode?.name!!)
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

            val sageOrganization = authClient.getSageOrganizationAccountType(
                SageOrganizationAccountTypeRequest(
                    paymentDetails.orgSerialId.toString(),
                    if (paymentDetails.accMode == AccMode.AR) AresConstants.IMPORTER_EXPORTER else AresConstants.SERVICE_PROVIDER
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
            val paymentLineItemDetails = getPaymentLineItem(paymentDetails.accMode)

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
                    bankCodeDetails = getPaymentBankDetails(paymentDetails.cogoAccountNo!!)
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
            val sageSignSuffix = getSignIntOfSageForAccModeAndPaymentCode(paymentDetails.accMode, paymentDetails.paymentCode!!)
            val paymentCode = getPaymentCodeOfSageForAccMode(paymentDetails.accMode, paymentDetails.paymentCode!!)

            val bankDetails = CogoBankAccount.values().find { it.cogoAccountNo == paymentDetails.cogoAccountNo }
            if (!bankDetails?.cogoAccountNo.isNullOrEmpty()) {
                if (((paymentDetails.cogoAccountNo == bankDetails?.cogoAccountNo) && (paymentDetails.entityCode == bankCodeDetails["entityCode"]?.toInt()) && (paymentDetails.currency == bankCodeDetails["currency"])) || (paymentDetails.payMode == PayMode.RAZORPAY)) {
                    jvSageAccount = getGLCodeOfSageForAccMode(paymentDetails.accMode)
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
                    paymentCode,
                    paymentDetails.paymentNumValue!!,
                    sageOrganization.sageOrganizationId!!,
                    AresConstants.IND,
                    jvSageAccount!!,
                    bankCode,
                    paymentDetails.transactionDate!!,
                    currency!!,
                    entityCode!!,
                    sageSignSuffix,
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
                            "UMRNUM_0: ${paymentDetails.paymentNumValue} -> ${result.requestString}",
                            "Sage Payment Num Value not present -> ${result.response}",
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

    private fun getPaymentLineItem(accMode: AccMode): PaymentLineItem {
        return PaymentLineItem(
            accMode = when (accMode) {
                AccMode.AR -> JVSageControls.AR.value
                AccMode.CSD -> JVSageControls.CSD.value
                else -> JVSageControls.AP.value
            }
        )
    }

    private fun getGLCodeOfSageForAccMode(accMode: AccMode): String {
        val glCode = when (accMode) {
            AccMode.AR -> JVSageAccount.AR.value
            AccMode.CSD -> JVSageAccount.CSD.value
            else -> JVSageAccount.AP.value
        }
        return glCode
    }

    private fun getSignIntOfSageForAccModeAndPaymentCode(accMode: AccMode, paymentCode: PaymentCode): Int {
        val signInt = when (accMode) {
            AccMode.AR -> 2
            AccMode.CSD -> when (paymentCode) {
                PaymentCode.REC -> 2
                else -> 1
            }
            else -> 1
        }
        return signInt
    }

    private fun getPaymentCodeOfSageForAccMode(accMode: AccMode, paymentCode: PaymentCode): String {
        val paymentCodeForSage = when (accMode) {
            AccMode.AR -> PaymentCode.REC.name
            AccMode.CSD -> paymentCode.name
            else -> PaymentCode.PAY.name
        }
        return paymentCodeForSage
    }

    private fun getPaymentBankDetails(cogoAccountNo: String): HashMap<String, String> {
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
        var recordStatus: String = ""
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
                    val paymentNumOnSage = "Select STA_0 from $sageDatabase.PAYMENTH where NUM_0 = '${payment.sageRefNumber!!}'"
                    val resultForPaymentNumOnSageQuery = SageClient.sqlQuery(paymentNumOnSage)
                    val mappedResponse = ObjectMapper().readValue<MutableMap<String, Any?>>(resultForPaymentNumOnSageQuery)
                    val records = mappedResponse["recordset"] as? ArrayList<*>
                    if (records?.size != 0) {
                        val recordMap = records!!.toArray()[0] as HashMap<String, Any>
                        recordStatus = recordMap["STA_0"]!!.toString()
                        if (recordStatus == "9") {
                            createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, result.response, true)
                            paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.FINAL_POSTED, performedBy)
                        } else {
                            createThirdPartyAudit(id, "PostPaymentFromSage", result.requestString, "Can't final post on Sage -> ${result.response}", false)
                            paymentRepository.updatePaymentDocumentStatus(id, PaymentDocumentStatus.POSTED, performedBy)
                            failedIds.add(id)
                        }
                    }
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

    override suspend fun getARLedgerOrganizationAndEntityWise(req: ARLedgerRequest): List<ARLedgerResponse> {
        val ledgerSelectedDateWise = accountUtilizationRepository.getARLedger(
            AccMode.AR,
            req.orgId,
            req.entityCodes!!,
            req.startDate!!,
            req.endDate!!
        )
        var arLedgerResponse = accountUtilizationMapper.convertARLedgerJobDetailsResponseToARLedgerResponse(ledgerSelectedDateWise)
        val openingLedger = accountUtilizationRepository.getOpeningAndClosingLedger(AccMode.AR, req.orgId, req.entityCodes!!, req.startDate!!)
        var openingLedgerList: List<ARLedgerResponse> = listOf(
            ARLedgerResponse(
                transactionDate = "",
                documentType = "",
                documentNumber = AresConstants.OPENING_BALANCE,
                currency = "",
                amount = "",
                debit = openingLedger.debit,
                credit = openingLedger.credit,
                debitBalance = if (openingLedger.debit > openingLedger.credit) openingLedger.debit.minus(openingLedger.credit) else BigDecimal.ZERO,
                creditBalance = if (openingLedger.credit > openingLedger.debit) openingLedger.credit.minus(openingLedger.debit) else BigDecimal.ZERO,
                transactionRefNumber = "",
                shipmentDocumentNumber = "",
                houseDocumentNumber = ""
            )
        )
        val completeLedgerList = openingLedgerList + arLedgerResponse

        for (index in 1..completeLedgerList.lastIndex) {
            val balance = (completeLedgerList[index].debit - completeLedgerList[index].credit) + (completeLedgerList[index - 1].debitBalance - completeLedgerList[index - 1].creditBalance)
            if (balance.compareTo(BigDecimal.ZERO) == 1) {
                completeLedgerList[index].debitBalance = balance
            } else {
                completeLedgerList[index].creditBalance = -balance
            }
        }
        var closingBalance = completeLedgerList[completeLedgerList.lastIndex].debitBalance - completeLedgerList[completeLedgerList.lastIndex].creditBalance
        var closingLedgerList: List<ARLedgerResponse> = listOf(
            ARLedgerResponse(
                transactionDate = "",
                documentType = "",
                documentNumber = AresConstants.CLOSING_BALANCE,
                currency = "",
                amount = "",
                debit = BigDecimal.ZERO,
                credit = BigDecimal.ZERO,
                debitBalance = if (closingBalance.compareTo(BigDecimal.ZERO) == 1) {
                    closingBalance
                } else {
                    BigDecimal.ZERO
                },
                creditBalance = if (closingBalance.compareTo(BigDecimal.ZERO) != 1) {
                    -closingBalance
                } else {
                    BigDecimal.ZERO
                },
                transactionRefNumber = "",
                shipmentDocumentNumber = "",
                houseDocumentNumber = ""
            )
        )
        return completeLedgerList + closingLedgerList
    }

    override suspend fun updateCSDPayments(request: UpdateCSDPaymentRequest) {
        when (request.status) {
            IncidentStatus.APPROVED.dbValue -> {
                val payment = paymentRepository.findByPaymentId(request.paymentId)
                val paymentModel = paymentConverter.convertToModel(payment)
                paymentModel.updatedBy = request.updatedBy.toString()
                paymentModel.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
                updatePaymentEntry(paymentModel)
            }
            IncidentStatus.REJECTED.dbValue -> {
                deletePaymentEntry(
                    DeletePaymentRequest(
                        paymentId = request.paymentId,
                        accMode = AccMode.AP
                    )
                )
            }
        }
    }

    private suspend fun validatingCreatePaymentRequest(req: Payment) {
        if (req.accMode == null) throw AresException(AresError.ERR_1009, "Acc Mode")

        if (req.docType != DocType.TDS && req.bankAccountNumber.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, "Bank Account")
        }

        if (req.accMode in listOf(AccMode.AR, AccMode.CSD)) {
            if (paymentRepository.isARTransRefNumberExists(accMode = req.accMode!!.name, transRefNumber = req.utr!!)) {
                throw AresException(AresError.ERR_1537, "")
            }
        }

        if (req.accMode == AccMode.CSD) {
            if (req.paymentCode == null) throw AresException(AresError.ERR_1003, "Payment Code")
        }
    }

    private suspend fun validatingUpdatePaymentRequest(receivableRequest: Payment): com.cogoport.ares.api.payment.entity.Payment {
        receivableRequest.accMode?.name ?: throw AresException(AresError.ERR_1003, "accMode")

        if (receivableRequest.transactionDate != null && receivableRequest.transactionDate!! > Date()) {
            throw AresException(AresError.ERR_1009, "Transaction date can't be of future")
        }

        receivableRequest.paymentCode?.name ?: throw AresException(AresError.ERR_1003, "paymentCode")
        val payment = receivableRequest.id?.let { paymentRepository.findByPaymentId(it) } ?: throw AresException(AresError.ERR_1002, "Payment")

        if (payment.paymentDocumentStatus == PaymentDocumentStatus.APPROVED) throw AresException(AresError.ERR_1010, "")

        return payment
    }

    override suspend fun saasInvoiceHook(req: SaasInvoiceHookRequest): SaasInvoiceHookResponse {
        plutusClient.updateStatus(
            InvoiceStatusUpdateRequest(
                id = Hashids.encode(req.proformaId!!),
                status = InvoiceStatus.FINANCE_ACCEPTED,
                invoiceDate = Date(),
                performedBy = req.performedBy,
                performedByUserType = req.performedByUserType
            )
        )
        val ledgerEntity = AresConstants.ENTITY_401
        val proformaRecord = accountUtilizationRepository.findRecord(req.proformaId!!, AccountType.SINV.toString(), AccMode.AR.toString())
        var totalAmount = BigDecimal(0)
        val paymentIds: MutableList<Long> = mutableListOf()
        var exchangeRate = BigDecimal(1.0)
        val ledgerCurrency = AresConstants.LEDGER_CURRENCY[req.entityCode]
        req.utrDetails?.forEach { utrDetail ->
            if (ledgerCurrency != req.currency) {
                exchangeRate = settlementServiceHelper.getExchangeRate(req.currency!!, proformaRecord?.ledCurrency!!, SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(utrDetail.transactionDate))
            }
            totalAmount = totalAmount.plus(utrDetail.paidAmount!!)
            // change payment ledger amount if doesn't matches with profroma
            val signFlag: Short = 1
            var paymentModel = Payment(
                entityType = req.entityCode,
                orgSerialId = proformaRecord?.orgSerialId,
                organizationId = proformaRecord?.organizationId,
                taggedOrganizationId = proformaRecord?.taggedOrganizationId,
                tradePartyMappingId = proformaRecord?.tradePartyMappingId,
                organizationName = proformaRecord?.organizationName,
                accMode = proformaRecord?.accMode,
                accCode = proformaRecord?.accCode,
                signFlag = signFlag,
                currency = req.currency,
                amount = utrDetail.paidAmount,
                ledCurrency = AresConstants.LEDGER_CURRENCY[req.entityCode],
                ledAmount = utrDetail.paidAmount!!.multiply(exchangeRate),
                utr = utrDetail.utrNumber,
                bankAccountNumber = req?.bankAccountNumber,
                bankName = req?.bankName,
                performedByUserType = req.performedByUserType,
                bankId = req?.bankId,
                transactionDate = utrDetail.transactionDate,
                paymentNum = null,
                paymentNumValue = null,
                refAccountNo = null,
                serviceType = null,
                createdBy = req.performedBy.toString(),
                updatedBy = req.performedBy.toString(),
                paymentDocumentStatus = PaymentDocumentStatus.APPROVED
            )
            val paymentId = createNonSuspensePaymentEntry(paymentModel)
            paymentIds.add(paymentId)
        }
        if (req.entityCode != ledgerEntity) {
            val bucketGlcode = parentJVService.getGLCodeMaster(AccMode.AR, entityCode = AresConstants.ENTITY_401, pageLimit = 1, q = null).filter { it.ledAccount == "SGP" }[0].accountCode
            val debitGlcode = parentJVService.getGLCodeMaster(AccMode.AR, entityCode = req.entityCode, pageLimit = 1, q = null).filter {
                when {
                    req.currency == "INR" -> it.ledAccount == "IND"
                    req.currency == "USD" -> it.ledAccount == "USD"
                    else -> false
                }
            }[0].accountCode
            if (proformaRecord != null) {
                settleInterEntity(
                    proformaId = proformaRecord.id!!,
                    paymentIds = paymentIds!!,
                    amount = proformaRecord.amountCurr!!,
                    currency = req.currency!!,
                    ledgerCurrency = proformaRecord.ledCurrency,
                    bucketGlcode = bucketGlcode,
                    debitGlcode = debitGlcode,
                    accMode = AccMode.AR,
                    debitEntityCode = req.entityCode!!,
                    ledgerEntityCode = AresConstants.ENTITY_401,
                    tradePartyDetails = OrgDetail(
                        proformaRecord.organizationName!!,
                        proformaRecord.organizationId!!,
                        req.entityCode, UUID.fromString(AresConstants.ENTITY_ID[req.entityCode])
                    ),
                    performedBy = req.performedBy!!,
                )
            }
        } else {
            paymentIds.forEach { paymentId ->
                if (proformaRecord != null) {
                    aresMessagePublisher.emitSendPaymentDetailsForKnockOff(
                        AutoKnockOffRequest(
                            paymentIdAsSourceId = Hashids.encode(paymentId),
                            destinationId = Hashids.encode(proformaRecord.documentNo),
                            sourceType = AccountType.REC.name,
                            destinationType = AccountType.SINV.name,
                            createdBy = req.performedBy!!
                        )
                    )
                }
            }
        }
        return SaasInvoiceHookResponse(id = paymentIds!!)
    }

    private suspend fun settleInterEntity(
        proformaId: Long,
        paymentIds: List<Long>,
        amount: BigDecimal,
        currency: String,
        ledgerCurrency: String,
        bucketGlcode: Int?,
        debitGlcode: Int?,
        accMode: AccMode,
        debitEntityCode: Int,
        ledgerEntityCode: Int,
        tradePartyDetails: OrgDetail,
        performedBy: UUID

    ) {
        val jvCodeNum = SettlementType.INTER.name
        val jvLineItem = mutableListOf(
            JvLineItemRequest(
                id = null,
                entityCode = ledgerEntityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[ledgerEntityCode]),
                accMode = accMode,
                tradePartyName = tradePartyDetails.tradePartyName,
                tradePartyId = tradePartyDetails.tradePartyId,
                type = "CREDIT",
                amount = amount,
                validityDate = Date(),
                glCode = bucketGlcode.toString(),
                currency = currency
            ),
            JvLineItemRequest(
                id = null,
                entityCode = debitEntityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[debitEntityCode]),
                accMode = accMode,
                tradePartyName = tradePartyDetails.tradePartyName,
                tradePartyId = tradePartyDetails.tradePartyId,
                type = "DEBIT",
                amount = amount,
                currency = currency,
                validityDate = Date(),
                glCode = debitGlcode.toString(),
            )
        )
        val jvRequest = ParentJournalVoucherRequest(
            id = null,
            jvCategory = SettlementType.INTER.name,
            transactionDate = Date(),
            currency = currency,
            ledCurrency = ledgerCurrency,
            entityCode = debitEntityCode,
            jvCodeNum = jvCodeNum,
            exchangeRate = BigDecimal(1),
            description = "Inter Entity Settlement of ${Hashids.encode(proformaId)}",
            jvLineItems = jvLineItem,
            createdBy = performedBy,
            entityId = UUID.fromString(AresConstants.ENTITY_ID[debitEntityCode]),
        )
        val creditedEntityJv = parentJVService.createJournalVoucher(jvRequest)
        val jvs = journalVoucherRepository.findByJvNums(listOf(creditedEntityJv!!))
        if (jvs != null) {
            // settle debit jv with payments
            paymentIds.forEach { paymentId ->
                aresMessagePublisher.emitSendPaymentDetailsForKnockOff(
                    AutoKnockOffRequest(
                        paymentIdAsSourceId = Hashids.encode(paymentId),
                        destinationId = Hashids.encode(jvs.filter { it.type == "DEBIT" }[0].id!!),
                        createdBy = performedBy,
                        sourceType = AccountType.REC.name,
                        destinationType = AccountType.valueOf(jvs.filter { it.type == "DEBIT" }[0].category!!).toString()
                    )
                )
            }

            // settle credit jv with invoice
            aresMessagePublisher.emitSendPaymentDetailsForKnockOff(
                AutoKnockOffRequest(
                    paymentIdAsSourceId = Hashids.encode(jvs.filter { it.type == "CREDIT" }[0].id!!),
                    destinationId = Hashids.encode(proformaId),
                    sourceType = AccountType.valueOf(jvs.filter { it.type == "CREDIT" }[0].category!!).name,
                    destinationType = AccountType.SINV.name,
                    createdBy = performedBy
                )
            )
        }
    }
    companion object {
        data class OrgDetail(
            var tradePartyName: String?,
            var tradePartyId: UUID?,
            var entityCode: Int?,
            var entityId: UUID?
        )
    }

    @Transactional
    override suspend fun updateVendorTradePartyData(
        request: UpdateOrganizationDetailAresSideRequest
    ): MutableMap<String, String>? {
        var response: MutableMap<String, String>? = mutableMapOf()

        response?.put(
            key = "requestBody",
            value = ObjectMapper().writeValueAsString(request)
        )

        try {
            var accountUtilizationDetail = accountUtilizationRepository.getAccountUtilizationByDocNoAndAccMode(
                documentNo = request.billId,
                accMode = AP.name,
                documentValue = request.billNumber,
                accType = EXP.name,
                serviceType = EXPENSE
            ) ?: throw AresException(ERR_1002, " account utilization not found.")

            accountUtilizationDetail.organizationId = request.organizationTradePartyDetailId ?: accountUtilizationDetail.organizationId
            accountUtilizationDetail.organizationName = request.organizationTradePartyName ?: accountUtilizationDetail.organizationName
            accountUtilizationDetail.taggedOrganizationId = request.organizationId ?: accountUtilizationDetail.taggedOrganizationId
            accountUtilizationDetail.tradePartyMappingId = request.organizationTradePartiesId ?: accountUtilizationDetail.tradePartyMappingId
            accountUtilizationDetail.orgSerialId = request.organizationTradePartySerialId ?: accountUtilizationDetail.orgSerialId

            accountUtilizationRepository.update(accountUtilizationDetail)

            response?.put(
                key = "accountUtilizationDetail",
                value = ObjectMapper().writeValueAsString(accountUtilizationDetail)
            )

            if (request.settlementNumbers != null && request.settlementNumbers!!.isNotEmpty()) {
                val settlements = settlementRepository.getSettlementDataUsingSettlementNumAndDestinationType(
                    settlementNum = request.settlementNumbers!!,
                    destinationType = PINV
                )
                val destinationIds: List<Long> = settlements.map { it.destinationId }

                val allSettlements = settlementRepository.getSettlementUsingDestinationIdsAndType(
                    destIds = destinationIds,
                    destinationType = PINV
                )

                val paymentNums: MutableList<Long> = mutableListOf()
                val journalVoucherIds: MutableList<Long> = mutableListOf()

                allSettlements.forEach { settlement ->
                    if (settlement.sourceType == SettlementType.PAY.name && settlement.sourceId != null) paymentNums.add(settlement.sourceId!!)
                    if (settlement.sourceType == SettlementType.VTDS.name && settlement.sourceId != null) journalVoucherIds.add(settlement.sourceId!!)
                }

                if (paymentNums != null) {
                    val payments = paymentRepository.getPaymentByPaymentNums(
                        paymentNums = paymentNums,
                        accMode = AP.name,
                        paymentCode = PAY.name
                    )

                    payments.forEach { payment ->
                        payment.organizationId = request.organizationTradePartyDetailId ?: payment.organizationId
                        payment.organizationName = request.organizationTradePartyName ?: payment.organizationName
                        payment.taggedOrganizationId = request.organizationId ?: payment.taggedOrganizationId
                        payment.orgSerialId = request.organizationTradePartySerialId ?: payment.orgSerialId
                        payment.tradePartyMappingId = request.organizationTradePartiesId ?: payment.tradePartyMappingId
                        payment.updatedBy = request.updatedBy

                        paymentRepository.update(payment)

                        response?.put(
                            key = "payment => ${payment.id}",
                            value = ObjectMapper().writeValueAsString(payment)
                        )

                        if (payment.paymentNum != null && payment.paymentNumValue != null) {
                            val accountUtilizationForPayment = accountUtilizationRepository.getAccountUtilizationByDocNoAndAccMode(
                                documentNo = payment.paymentNum!!,
                                documentValue = payment.paymentNumValue!!,
                                accMode = AP.name,
                                accType = PAY.name,
                                serviceType = EXPENSE
                            )

                            if (accountUtilizationForPayment != null) {
                                accountUtilizationForPayment?.organizationId = request.organizationTradePartyDetailId ?: accountUtilizationDetail?.organizationId
                                accountUtilizationForPayment?.organizationName = request.organizationTradePartyName ?: accountUtilizationDetail?.organizationName
                                accountUtilizationForPayment?.taggedOrganizationId = request.organizationId ?: accountUtilizationDetail?.taggedOrganizationId
                                accountUtilizationForPayment?.orgSerialId = request.organizationTradePartySerialId ?: accountUtilizationDetail?.orgSerialId
                                accountUtilizationForPayment?.tradePartyMappingId = request.organizationTradePartiesId ?: accountUtilizationDetail?.tradePartyMappingId
                                accountUtilizationRepository.update(accountUtilizationForPayment)

                                response?.put(
                                    key = "accountUtilizationForPayment => ${accountUtilizationForPayment.id}",
                                    value = ObjectMapper().writeValueAsString(accountUtilizationForPayment)
                                )
                            }
                        }
                    }

                    if (journalVoucherIds != null) {
                        journalVoucherIds.forEach { journalVoucherId ->
                            val journalVoucher = journalVoucherRepository.findById(journalVoucherId)

                            if (journalVoucher != null) {
                                journalVoucher.tradePartyId = request.organizationTradePartyDetailId ?: journalVoucher.tradePartyId
                                journalVoucher.tradePartyName = request.organizationTradePartyName ?: journalVoucher.tradePartyName
                                journalVoucher.updatedBy = request.updatedBy

                                journalVoucherRepository.update(journalVoucher)

                                response?.put(
                                    key = "journalVoucher => ${journalVoucher.id}",
                                    value = ObjectMapper().writeValueAsString(journalVoucher)
                                )

                                val accountUtilizationForJournalVoucher = accountUtilizationRepository.getAccountUtilizationByDocNoAndAccMode(
                                    documentValue = journalVoucher.jvNum,
                                    documentNo = journalVoucher.id!!,
                                    accMode = AP.name,
                                    accType = VTDS.name,
                                    serviceType = EXPENSE
                                )

                                if (accountUtilizationForJournalVoucher != null) {
                                    accountUtilizationForJournalVoucher?.organizationId = request.organizationTradePartyDetailId ?: accountUtilizationForJournalVoucher.organizationId
                                    accountUtilizationForJournalVoucher?.organizationName = request.organizationTradePartyName ?: accountUtilizationForJournalVoucher.organizationName
                                    accountUtilizationForJournalVoucher?.taggedOrganizationId = request.organizationId ?: accountUtilizationForJournalVoucher.taggedOrganizationId
                                    accountUtilizationForJournalVoucher?.orgSerialId = request.organizationTradePartySerialId ?: accountUtilizationForJournalVoucher.orgSerialId
                                    accountUtilizationForJournalVoucher?.tradePartyMappingId = request.organizationTradePartiesId ?: accountUtilizationForJournalVoucher.tradePartyMappingId
                                    accountUtilizationRepository.update(accountUtilizationForJournalVoucher)

                                    response?.put(
                                        key = "accountUtilizationForJournal => ${accountUtilizationForJournalVoucher.id}",
                                        value = ObjectMapper().writeValueAsString(accountUtilizationForJournalVoucher)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw AresException(ERR_1002, ObjectMapper().writeValueAsString(ObjectMapper().writeValueAsString(request) + e))
        }

        logger().info("***** update vendor trade party data response $response")

        return response
    }
}
