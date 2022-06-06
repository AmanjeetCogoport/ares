package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.common.models.Messages
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
import com.cogoport.ares.model.payment.ZoneCode
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import javax.transaction.Transactional
import kotlin.math.ceil

@Singleton
open class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse {
        val data = OpenSearchClient().onAccountSearch(request, Payment::class.java)!!
        val payments = data.hits().hits().map { it.source() }
        val total = data.hits().total().value().toInt()
        return AccountCollectionResponse(payments = payments, totalRecords = total, totalPage = ceil(total.toDouble() / request.pageLimit.toDouble()).toInt(), page = request.page)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {

        var payment = paymentConverter.convertToEntity(receivableRequest)
        paymentRepository.save(payment)
        var accUtilizationModel: AccUtilizationRequest =
            accUtilizationToPaymentConverter.convertEntityToModel(payment)

        var paymentModel = paymentConverter.convertToModel(payment)
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, payment.id.toString(), paymentModel)

        accUtilizationModel.zoneCode = receivableRequest.zone
        accUtilizationModel.serviceType = receivableRequest.serviceType
        accUtilizationModel.accType = AccountType.REC
        accUtilizationModel.currencyPayment = 0.toBigDecimal()
        accUtilizationModel.ledgerPayment = 0.toBigDecimal()
        accUtilizationModel.ledgerAmount = 0.toBigDecimal()
        accUtilizationModel.docStatus = DocumentStatus.FINAL
        var accUtilRes = accountUtilizationRepository.save(accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel))
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        return OnAccountApiCommonResponse(id = accUtilRes.id!!, message = Messages.PAYMENT_CREATED, isSuccess = true)
    }

    /**
     * @param Payment
     * @return Payment
     */
    override suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse {
        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }
        var accountUtilization = accountUtilizationRepository.findByDocumentNo(receivableRequest.id)
        if (payment!!.id == null) throw AresException(AresError.ERR_1002, "")
        if (payment != null && payment.isPosted && accountUtilization != null)
            throw AresException(AresError.ERR_1006, "")
        return updatePayment(receivableRequest, accountUtilization, payment)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updatePayment(receivableRequest: Payment, accountUtilization: AccountUtilization, payment: com.cogoport.ares.api.payment.entity.Payment?): OnAccountApiCommonResponse {

        var paymentDetails = paymentRepository.update(paymentConverter.convertToEntity(receivableRequest))
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, paymentDetails.id.toString(), paymentDetails)

        var accUtilRes = accountUtilizationRepository.update(updateAccountUtilizationEntry(accountUtilization, receivableRequest))
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }

        return OnAccountApiCommonResponse(id = accUtilRes.id!!, message = Messages.PAYMENT_UPDATED, isSuccess = true)
        //   return payment?.let { paymentConverter.convertToModel(it) }
    }

    override suspend fun deletePaymentEntry(paymentId: Long): OnAccountApiCommonResponse {

        var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findById(paymentId) ?: throw AresException(AresError.ERR_1001, "")
        if (payment.id == null) throw AresException(AresError.ERR_1002, "")
        if (payment.isDeleted)
            throw AresException(AresError.ERR_1007, "")
        payment.isDeleted = true
        var paymentResponse = paymentRepository.update(payment)
        Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, payment.id.toString(), paymentResponse)

        var accountUtilization = accountUtilizationRepository.findByDocumentNo(payment.id)
        val paymentModel = paymentConverter.convertToModel(payment)

        accountUtilization.documentStatus = DocumentStatus.CANCELLED
        var accUtilRes = accountUtilizationRepository.update(accountUtilization)
        Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)

        return OnAccountApiCommonResponse(id = paymentId, message = Messages.PAYMENT_DELETED, isSuccess = true)
    }

    // Will be removed via Mapper
    fun updateAccountUtilizationEntry(accountUtilization: AccountUtilization, receivableRequest: Payment): AccountUtilization {
        accountUtilization.zoneCode = ZoneCode.valueOf(receivableRequest.zone!!)
        accountUtilization.documentStatus = DocumentStatus.FINAL
        accountUtilization.serviceType = receivableRequest.serviceType.toString()
        accountUtilization.entityCode = receivableRequest.entityType
        // accountUtilization.category = "non_asset"
        accountUtilization.orgSerialId = receivableRequest.orgSerialId!!
        accountUtilization.organizationId = receivableRequest.customerId!!
        accountUtilization.organizationName = receivableRequest.customerName
        accountUtilization.sageOrganizationId = receivableRequest.sageOrganizationId
        accountUtilization.accCode = receivableRequest.accCode
        accountUtilization.accMode = receivableRequest.accMode!!
        accountUtilization.signFlag = receivableRequest.signFlag
        accountUtilization.amountCurr = receivableRequest.amount
        accountUtilization.amountLoc = receivableRequest.ledAmount
        accountUtilization.dueDate = receivableRequest.transactionDate!!
        accountUtilization.transactionDate = receivableRequest.transactionDate!!
        return accountUtilization
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse {

        var paymentEntityList = arrayListOf<com.cogoport.ares.api.payment.entity.Payment>()
        for (payment in bulkPayment) {
            payment.accMode = AccMode.AR
            payment.paymentCode = PaymentCode.REC
            paymentEntityList.add(paymentConverter.convertToEntity(payment))
            var savePayment = paymentRepository.save(paymentConverter.convertToEntity(payment))
            var accUtilizationModel: AccUtilizationRequest =
                accUtilizationToPaymentConverter.convertEntityToModel(savePayment)

            var paymentModel = paymentConverter.convertToModel(savePayment)
            Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savePayment.id.toString(), paymentModel)

            accUtilizationModel.zoneCode = payment.zone
            accUtilizationModel.serviceType = payment.serviceType
            accUtilizationModel.accType = AccountType.PAY
            accUtilizationModel.currencyPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerAmount = 0.toBigDecimal()
            accUtilizationModel.docStatus = DocumentStatus.FINAL
            var accUtilRes = accountUtilizationRepository.save(accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel))
            Client.addDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilRes.id.toString(), accUtilRes)
        }

        return BulkPaymentResponse(recordsInserted = bulkPayment.size)
    }
}
