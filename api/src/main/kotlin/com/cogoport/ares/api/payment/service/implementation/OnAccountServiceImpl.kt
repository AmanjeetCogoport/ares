package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.CogoClient
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
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentResponse
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import javax.transaction.Transactional
import kotlin.math.ceil

@Singleton
open class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var cogoClient: CogoClient

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

        /*PRIVATE FUNCTION TO SET AMOUNTS*/
        setPaymentAmounts(receivableRequest)

        setOrganizations(receivableRequest)

        var payment = paymentConverter.convertToEntity(receivableRequest)

        /*PRIVATE FUNCTION TO SET PAYMENT ENTITY*/
        setPaymentEntity(payment)

        /*GENERATING A UNIQUE RECEIPT NUMBER FOR PAYMENT*/
        payment.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
        payment.paymentNumValue = SequenceSuffix.RECEIVED.prefix + payment.paymentNum

        /*SAVING THE PAYMENT IN DATABASE*/
        val savedPayment = paymentRepository.save(payment)
        receivableRequest.id = savedPayment.id
        receivableRequest.isPosted = false
        receivableRequest.isDeleted = false
        receivableRequest.paymentNum = payment.paymentNum
        receivableRequest.paymentNumValue = payment.paymentNumValue
        receivableRequest.accCode = payment.accCode
        receivableRequest.paymentCode = payment.paymentCode

        /*SAVE THE PAYMENT IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savedPayment.id.toString(), receivableRequest)

        var accUtilizationModel: AccUtilizationRequest = accUtilizationToPaymentConverter.convertEntityToModel(payment)

        /*PRIVATE FUNCTION TO SET ACCOUNT UTILIZATION MODEL*/
        setAccountUtilizationModel(accUtilizationModel, receivableRequest)

        var accUtilEntity = accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel)

        accUtilEntity.accCode = AresModelConstants.AR_ACCOUNT_CODE
        accUtilEntity.documentNo = payment.paymentNum!!
        accUtilEntity.documentValue = payment.paymentNumValue

        if (receivableRequest.accMode == AccMode.AP) {
            accUtilEntity.accCode = AresModelConstants.AP_ACCOUNT_CODE
        }

        /*SAVE ACCOUNT UTILIZATION IN DATABASE AS ON ACCOUNT PAYMENT*/
        var accUtilRes = accountUtilizationRepository.save(accUtilEntity)

        /*SAVE THE ACCOUNT UTILIZATION IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        // Emitting Kafka message to Update Outstanding and Dashboard
        emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))

        return OnAccountApiCommonResponse(id = savedPayment.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    /**
     * Emit message to Kafka topic receivables-dashboard-data to update Dashboard and Receivables outstanding documents on OpenSearch
     * @param accUtilizationRequest
     */
    private fun emitDashboardAndOutstandingEvent(accUtilizationRequest: AccUtilizationRequest) {
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(accUtilizationRequest.dueDate),
                    quarter = accUtilizationRequest.dueDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().get(
                        IsoFields.QUARTER_OF_YEAR
                    ),
                    year = accUtilizationRequest.dueDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year,
                )
            )
        )
        aresKafkaEmitter.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    orgId = accUtilizationRequest.organizationId.toString()
                )
            )
        )
    }

    /**
     * @param Payment
     * @return Payment
     */
    override suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        var payment = receivableRequest.id?.let { paymentRepository.findByPaymentId(it) }

        var accountUtilization = accountUtilizationRepository.findRecord(payment?.paymentNum!!, AccountType.REC.name, AccMode.AR.name)

        if (payment!!.id == null) throw AresException(AresError.ERR_1002, "")

        if (payment != null && payment.isPosted && accountUtilization != null)
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

            setOrganizations(receivableRequest)

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
        var paymentDetails = paymentRepository.update(paymentEntity)
        val openSearchPaymentModel = paymentConverter.convertToModel(paymentDetails)
        openSearchPaymentModel.paymentDate = paymentDetails.transactionDate?.toLocalDate().toString()

        /*UPDATE THE OPEN SEARCH WITH UPDATED PAYMENT ENTRY*/
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentDetails.id.toString(), openSearchPaymentModel)

        /*UPDATE THE DATABASE WITH UPDATED ACCOUNT UTILIZATION ENTRY*/
        var accUtilRes = accountUtilizationRepository.update(accountUtilizationEntity)

        /*UPDATE THE OPEN SEARCH WITH UPDATED ACCOUNT UTILIZATION ENTRY */
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        // Emitting Kafka message to Update Outstanding and Dashboard
        emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))

        return OnAccountApiCommonResponse(id = accUtilRes.id!!, message = Messages.PAYMENT_UPDATED, isSuccess = true)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun deletePaymentEntry(paymentId: Long): OnAccountApiCommonResponse {

        var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findByPaymentId(paymentId) ?: throw AresException(AresError.ERR_1001, "")

        if (payment == null)
            throw AresException(AresError.ERR_1002, "")
        if (payment.isDeleted)
            throw AresException(AresError.ERR_1007, "")

        payment.isDeleted = true
        /*MARK THE PAYMENT AS DELETED IN DATABASE*/
        var paymentResponse = paymentRepository.update(payment)

        val openSearchPaymentModel = paymentConverter.convertToModel(paymentResponse)
        openSearchPaymentModel.paymentDate = paymentResponse.transactionDate?.toLocalDate().toString()

        /*MARK THE PAYMENT AS DELETED IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, payment.id.toString(), openSearchPaymentModel)

        var accountUtilization = accountUtilizationRepository.findRecord(payment.paymentNum!!, AccountType.REC.name, AccMode.AR.name) ?: throw AresException(AresError.ERR_1202, "")
        accountUtilization.documentStatus = DocumentStatus.DELETED

        /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN DATABASE*/
        var accUtilRes = accountUtilizationRepository.update(accountUtilization)

        /*MARK THE ACCOUNT UTILIZATION  AS DELETED IN OPEN SEARCH*/
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        // Emitting Kafka message to Update Outstanding and Dashboard
        emitDashboardAndOutstandingEvent(accountUtilizationMapper.convertToModel(accUtilRes))

        return OnAccountApiCommonResponse(id = paymentId, message = Messages.PAYMENT_DELETED, isSuccess = true)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse {

        var paymentEntityList = arrayListOf<com.cogoport.ares.api.payment.entity.Payment>()
        for (payment in bulkPayment) {
            payment.accMode = AccMode.AR
            payment.paymentCode = PaymentCode.REC
            payment.zone = null
            payment.serviceType = ServiceType.NA

            // TODO: Remove below commented code after mohit confirmation
//            val orgDetails = OpenSearchClient().orgDetailSearch(payment.orgSerialId!!)
//            val orgId = (orgDetails?.hits()?.hits()?.map { it.source() }?.get(0) as Map<String, Any>).map { it.value }.get(0)
//            payment.organizationId = UUID.fromString(orgId.toString())
            paymentEntityList.add(paymentConverter.convertToEntity(payment))
            payment.accCode = AresModelConstants.AR_ACCOUNT_CODE
            if (payment.accMode == AccMode.AP) {
                payment.accCode = AresModelConstants.AP_ACCOUNT_CODE
            }

            var savePayment = paymentRepository.save(paymentConverter.convertToEntity(payment))
            var accUtilizationModel: AccUtilizationRequest =
                accUtilizationToPaymentConverter.convertEntityToModel(savePayment)

            var paymentModel = paymentConverter.convertToModel(savePayment)
            paymentModel.paymentDate = paymentModel.transactionDate?.toLocalDate().toString()
            Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savePayment.id.toString(), paymentModel)
            accUtilizationModel.zoneCode = payment.zone
            accUtilizationModel.serviceType = payment.serviceType
            accUtilizationModel.accType = AccountType.PAY
            accUtilizationModel.currencyPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerAmount = payment.ledAmount
            accUtilizationModel.ledCurrency = payment.ledCurrency!!
            accUtilizationModel.docStatus = DocumentStatus.FINAL
            var accUtilEntity = accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel)
            accUtilEntity.accCode = payment.accCode!!
            var accUtilRes = accountUtilizationRepository.save(accUtilEntity)
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
        }

        return BulkPaymentResponse(recordsInserted = bulkPayment.size)
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
        val clientResponse = cogoClient.getCogoOrganization(receivableRequest.organizationId.toString())

        if(clientResponse==null || clientResponse.organizationSerialId==null){
            throw AresException(AresError.ERR_1202,"")
        }
        receivableRequest.orgSerialId = clientResponse.organizationSerialId
        receivableRequest.organizationName = clientResponse.organizationName
        receivableRequest.zone = clientResponse.zone?.uppercase()
    }
}
