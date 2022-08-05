package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.mapper.OrgStatsMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.model.AuditAccountUtilizationRequest
import com.cogoport.ares.api.payment.model.AuditPaymentRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.MappingIdDetailRequest
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.TradePartyDetailRequest
import com.cogoport.ares.model.payment.TradePartyOrganizationResponse
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.PaymentResponse
import com.cogoport.ares.model.payment.response.PlatformOrganizationResponse
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil

@Singleton
open class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var accountUtilizationMapper: AccountUtilizationMapper

    @Inject
    lateinit var orgStatsConverter: OrgStatsMapper

    @Inject
    lateinit var auditService: AuditService

    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse {
        val data = OpenSearchClient().onAccountSearch(request, PaymentResponse::class.java)!!
        val payments = data.hits().hits().map { it.source() }
        val total = data.hits().total().value().toInt()
        return AccountCollectionResponse(list = payments, totalRecords = total, totalPage = ceil(total.toDouble() / request.pageLimit.toDouble()).toInt(), page = request.page)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
        val filterDateFromTs = Timestamp(dateFormat.parse(receivableRequest.paymentDate).time)
        receivableRequest.transactionDate = filterDateFromTs
        receivableRequest.serviceType = ServiceType.NA
        receivableRequest.accMode = AccMode.AR
        receivableRequest.signFlag = SignSuffix.REC.sign

        setPaymentAmounts(receivableRequest)
//        setOrganizations(receivableRequest)
//        setTradePartyOrganizations(receivableRequest)
        setTradePartyInfo(receivableRequest)

        val payment = paymentConverter.convertToEntity(receivableRequest)
        setPaymentEntity(payment)
        payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
        payment.paymentNumValue = SequenceSuffix.RECEIVED.prefix + payment.paymentNum

        val savedPayment = paymentRepository.save(payment)
        auditService.auditPayment(AuditPaymentRequest(payment, AresConstants.CREATE, receivableRequest.createdBy, receivableRequest.performedByUserType))
        receivableRequest.id = savedPayment.id
        receivableRequest.isPosted = false
        receivableRequest.isDeleted = false
        receivableRequest.paymentNum = payment.paymentNum
        receivableRequest.paymentNumValue = payment.paymentNumValue
        receivableRequest.accCode = payment.accCode
        receivableRequest.paymentCode = payment.paymentCode

        val accUtilizationModel: AccUtilizationRequest = accUtilizationToPaymentConverter.convertEntityToModel(payment)

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
        auditService.auditAccountUtilization(AuditAccountUtilizationRequest(accUtilEntity, "create", receivableRequest.createdBy, receivableRequest.performedByUserType))
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest, true)

        try {
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
        }
        return OnAccountApiCommonResponse(id = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    /**
     *
     */
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
    private fun emitDashboardAndOutstandingEvent(accUtilizationRequest: AccUtilizationRequest) {
        val date = accUtilizationRequest.dueDate ?: accUtilizationRequest.transactionDate
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(date),
                    quarter = date!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().get(IsoFields.QUARTER_OF_YEAR),
                    year = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year,
                    accMode = accUtilizationRequest.accMode
                )
            )
        )
        aresKafkaEmitter.emitOutstandingData(
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
        val payment = receivableRequest.id?.let { paymentRepository.findByPaymentId(it) } ?: throw AresException(AresError.ERR_1002, "")

        val accountUtilization = accountUtilizationRepository.findRecord(payment.paymentNum!!, AccountType.REC.name, AccMode.AR.name)

        if (payment.isPosted && accountUtilization != null)
            throw AresException(AresError.ERR_1005, "")

        return updatePayment(receivableRequest, accountUtilization!!, payment)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updatePayment(receivableRequest: Payment, accountUtilizationEntity: AccountUtilization, paymentEntity: com.cogoport.ares.api.payment.entity.Payment): OnAccountApiCommonResponse {

        if (receivableRequest.isPosted != null && receivableRequest.isPosted == true) {
            paymentEntity.isPosted = true
            accountUtilizationEntity.documentStatus = DocumentStatus.FINAL
        } else {

            val dateFormat = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT)
            val filterDateFromTs = Timestamp(dateFormat.parse(receivableRequest.paymentDate).time)

//            setOrganizations(receivableRequest)
//            setTradePartyOrganizations(receivableRequest)
            setTradePartyInfo(receivableRequest)

            /*SET PAYMENT ENTITY DATA FOR UPDATE*/
            paymentEntity.entityCode = receivableRequest.entityType!!
            paymentEntity.bankName = receivableRequest.bankName
            paymentEntity.payMode = receivableRequest.payMode
            paymentEntity.transactionDate = filterDateFromTs
            paymentEntity.transRefNumber = receivableRequest.utr
            paymentEntity.amount = receivableRequest.amount!!
            paymentEntity.currency = receivableRequest.currency!!
            paymentEntity.ledAmount = receivableRequest.amount!! * receivableRequest.exchangeRate!!
            paymentEntity.ledCurrency = receivableRequest.ledCurrency
            paymentEntity.exchangeRate = receivableRequest.exchangeRate
            paymentEntity.orgSerialId = receivableRequest.orgSerialId
            paymentEntity.organizationId = receivableRequest.organizationId
            paymentEntity.organizationName = receivableRequest.organizationName
            paymentEntity.bankName = receivableRequest.bankName
            paymentEntity.cogoAccountNo = receivableRequest.bankAccountNumber
            paymentEntity.updatedAt = Timestamp.from(Instant.now())
            paymentEntity.bankId = receivableRequest.bankId

            /*SET ACCOUNT UTILIZATION DATA FOR UPDATE*/
            accountUtilizationEntity.entityCode = receivableRequest.entityType!!
            accountUtilizationEntity.orgSerialId = receivableRequest.orgSerialId
            accountUtilizationEntity.organizationId = receivableRequest.organizationId
            accountUtilizationEntity.organizationName = receivableRequest.organizationName
            accountUtilizationEntity.transactionDate = paymentEntity.transactionDate
            accountUtilizationEntity.amountCurr = receivableRequest.amount!!
            accountUtilizationEntity.currency = receivableRequest.currency!!
            accountUtilizationEntity.amountLoc = receivableRequest.ledAmount!!
            accountUtilizationEntity.ledCurrency = receivableRequest.ledCurrency!!
            accountUtilizationEntity.updatedAt = Timestamp.from(Instant.now())
            accountUtilizationEntity.zoneCode = receivableRequest.zone
        }

        /*UPDATE THE DATABASE WITH UPDATED PAYMENT ENTRY*/
        val paymentDetails = paymentRepository.update(paymentEntity)
        auditService.auditPayment(AuditPaymentRequest(paymentEntity, AresConstants.UPDATE, receivableRequest.createdBy, receivableRequest.performedByUserType))
        val openSearchPaymentModel = paymentConverter.convertToModel(paymentDetails)
        openSearchPaymentModel.paymentDate = paymentDetails.transactionDate?.toLocalDate().toString()
        openSearchPaymentModel.uploadedBy = receivableRequest.uploadedBy

        /*UPDATE THE DATABASE WITH UPDATED ACCOUNT UTILIZATION ENTRY*/
        val accUtilRes = accountUtilizationRepository.update(accountUtilizationEntity)
        auditService.auditAccountUtilization(AuditAccountUtilizationRequest(accountUtilizationEntity, "update", receivableRequest.createdBy, receivableRequest.performedByUserType))
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

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse {

        val payment = paymentRepository.findByPaymentId(deletePaymentRequest.paymentId) ?: throw AresException(AresError.ERR_1001, "")

        if (payment.isDeleted)
            throw AresException(AresError.ERR_1007, "")

        payment.isDeleted = true
        /*MARK THE PAYMENT AS DELETED IN DATABASE*/
        val paymentResponse = paymentRepository.update(payment)
        auditService.auditPayment(AuditPaymentRequest(payment, AresConstants.DELETE, deletePaymentRequest.performedById, deletePaymentRequest.performedByUserType))
        val openSearchPaymentModel = paymentConverter.convertToModel(paymentResponse)
        openSearchPaymentModel.paymentDate = paymentResponse.transactionDate?.toLocalDate().toString()

        val accountUtilization = accountUtilizationRepository.findRecord(payment.paymentNum!!, AccountType.REC.name, AccMode.AR.name) ?: throw AresException(AresError.ERR_1202, "")
        accountUtilization.documentStatus = DocumentStatus.DELETED

        /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN DATABASE*/
        val accUtilRes = accountUtilizationRepository.update(accountUtilization)
        auditService.auditAccountUtilization(AuditAccountUtilizationRequest(accountUtilization, "delete", deletePaymentRequest.performedById, deletePaymentRequest.performedByUserType))
        /*MARK THE PAYMENT AS DELETED IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, payment.id.toString(), openSearchPaymentModel)

        try {
            /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN OPEN SEARCH*/
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
            // Emitting Kafka message to Update Outstanding and Dashboard
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

    private fun setPaymentEntity(payment: com.cogoport.ares.api.payment.entity.Payment) {
        payment.accCode = AresModelConstants.AR_ACCOUNT_CODE
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())
        payment.isPosted = false
        payment.isDeleted = false
        payment.paymentCode = PaymentCode.REC
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

    override suspend fun getOrgStats(orgId: UUID?): OrgStatsResponse {
        if (orgId == null) throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        val response = accountUtilizationRepository.getOrgStats(orgId) ?: throw AresException(AresError.ERR_1005, "")
        return orgStatsConverter.convertToModel(response)
    }

    private suspend fun setTradePartyInfo(receivableRequest: Payment) {
        val clientResponse: TradePartyOrganizationResponse?

        val reqBody = MappingIdDetailRequest(
            receivableRequest?.tradePartyMappingId.toString()
        )

        clientResponse = authClient.getTradePartyInfo(reqBody)

        if (clientResponse.organizationTradePartySerialId == null) {
            throw AresException(AresError.ERR_1207, "")
        }
        receivableRequest.orgSerialId = clientResponse.organizationTradePartySerialId
        receivableRequest.organizationName = clientResponse.organizationTradePartyName
        receivableRequest.zone = clientResponse.organizationTradePartyZone?.uppercase()
        receivableRequest.organizationId = clientResponse.organizationTradePartyDetailId
    }
}
