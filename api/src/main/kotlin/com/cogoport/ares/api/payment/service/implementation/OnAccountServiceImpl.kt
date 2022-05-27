package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.model.payment.*
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.s3.client.S3Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.FileReader
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

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

    @Inject
    private lateinit var s3Client: S3Client
    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(
        uploadedDate: LocalDateTime?,
        entityType: Int?,
        currencyType: String?
    ): AccountCollectionResponse {
        var payments = mutableListOf<Payment>()
        var data = paymentRepository.listOrderByCreatedAtDesc()
        data.forEach {
            val payment = paymentConverter.convertToModel(it)
            payments.add(payment)
        }
        return AccountCollectionResponse(payments = payments)
    }

    // Need to make this trasactional
    override suspend fun createPaymentEntry(receivableRequest: Payment): Payment {

        var payment = paymentConverter.convertToEntity(receivableRequest)
        paymentRepository.save(payment)

        var accountUtilization = AccountUtilization(
            id = null,
            documentNo = payment.id!!,
            documentValue = "invoice",
            zoneCode = "NORTH",
            serviceType = "FCL_FREIGHT",
            docStatus = "FINAL",
            entityCode = payment.entityCode,
            category = "asset",
            orgSerialId = payment.orgSerialId!!,
            sageOrganizationId = payment.sageOrganizationId,
            organizationId = payment.organizationId,
            organizationName = payment.organizationName,
            accCode = payment.accCode,
            accType = AccountType.REC,
            accMode = payment.accMode,
            signFlag = payment.signFlag,
            currency = payment.currency,
            ledCurrency = payment.currency,
            amountCurr = payment.amount,
            amountLoc = payment.ledAmount,
            dueDate = payment.transactionDate!!,
            transactionDate = payment.transactionDate!!
        )

        accountUtilizationRepository.save(accountUtilization)
        return paymentConverter.convertToModel(payment)
    }

    /**
     * @param Payment
     * @return Payment
     */
    override suspend fun updatePaymentEntry(receivableRequest: Payment): Payment? {
        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }
        var accountUtilization = accountUtilizationRepository.findByPaymentId(receivableRequest.id)
        if (payment != null && payment.isPosted && accountUtilization != null)
            throw AresException(AresError.ERR_1006, "")

        return updatePayment(receivableRequest, accountUtilization, payment)
    }

    /**
     *
     */
    private suspend fun updatePayment(receivableRequest: Payment, accountUtilization: AccountUtilization, payment: com.cogoport.ares.api.payment.entity.Payment?): Payment? {
        paymentRepository.update(paymentConverter.convertToEntity(receivableRequest))
        accountUtilizationRepository.update(updateAccountUtilizationEntry(accountUtilization, receivableRequest))
//        return payment?.let { paymentConverter.convertToModel(it) }
        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }
        return payment?.let { paymentConverter.convertToModel(it) }
    }

//    override suspend fun updatePostOnPaymentEntry(paymentId: Long): Long? {
//        try {
//            var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findById(paymentId) ?: throw AresException(AresError.ERR_1001, "")
//
//            if (payment.isPosted)
//                throw AresException(AresError.ERR_1001, "")
//
//            payment.isPosted = true
//            paymentRepository.update(payment)
//            return payment.id
//        } catch (e: Exception) {
//            throw e
//        }
//    }

    override suspend fun deletePaymentEntry(paymentId: Long): String? {
        try {
            var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findById(paymentId) ?: throw AresException(AresError.ERR_1001, "")

            if (payment.isDeleted)
                throw AresException(AresError.ERR_1001, "")

            payment.isDeleted = true
            paymentRepository.update(payment)
            return "Successfully Deleted!"
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun upload(): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun updateAccountUtilizationEntry(accountUtilization: AccountUtilization, receivableRequest: Payment): AccountUtilization {
        accountUtilization.zoneCode = "NORTH"
        accountUtilization.docStatus = "FINAL"
        accountUtilization.serviceType = "FCL_FREIGHT"
        accountUtilization.entityCode = receivableRequest.entityType
        accountUtilization.category = "non_asset"
        accountUtilization.orgSerialId = receivableRequest.orgSerialId!!
        accountUtilization.organizationId = receivableRequest.customerId
        accountUtilization.organizationName = receivableRequest.customerName
        accountUtilization.sageOrganizationId = receivableRequest.sageOrganizationId
        accountUtilization.accCode = receivableRequest.accCode
        accountUtilization.accMode = receivableRequest.accMode
        accountUtilization.signFlag = receivableRequest.signFlag
        accountUtilization.amountCurr = receivableRequest.amount
        accountUtilization.amountLoc = receivableRequest.ledAmount
        accountUtilization.dueDate = receivableRequest.transactionDate!!
        accountUtilization.transactionDate = receivableRequest.transactionDate!!
        return accountUtilization
    }

    @Transactional
    override suspend fun createBulkOnAccountPayments(): Void? {

//        val file = File("/Users/mohitmogera/Documents/Projects/Kotlin/ares/build/resources/main/invoices1.csv")
//        var s3Response =  s3Client.upload("business-finance-test", "invoices1.csv", file)
        var docUrl =  s3Client.download("business-finance-test","invoices1.csv")
        val bufferedReader = FileReader(docUrl)

        val csvParser = CSVParser(
            bufferedReader,
            CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
        )

        // Validate excel List before passing it for insertion

        var paymentModelList: MutableList<Payment> = convertRowToObject(csvParser)
        var paymentEntityList = arrayListOf<com.cogoport.ares.api.payment.entity.Payment>()
        for (payment in paymentModelList) {

            paymentEntityList.add(paymentConverter.convertToEntity(payment))
            var savePayment = paymentRepository.save(paymentConverter.convertToEntity(payment))
            var accUtilizationModel: AccUtilizationRequest =
                accUtilizationToPaymentConverter.convertEntityToModel(savePayment)

            var paymentModel = paymentConverter.convertToModel(savePayment)
            Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX,savePayment.id.toString(),paymentModel)
            accUtilizationModel.accType = "PAY"
            accUtilizationModel.currencyPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerAmount = 0.toBigDecimal()
            accountUtilizationRepository.save(accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel))
        }
        return null
    }

    private fun convertRowToObject(csvParser: CSVParser): MutableList<Payment> {
        var paymentList = mutableListOf<Payment>()
        var recordCount = 1
        for (csvRecord in csvParser) {
            paymentList.add(
                Payment(
                    customerName = csvRecord.get(0),
                    customerId = UUID.fromString(csvRecord.get(1)),
                    entityType = csvRecord.get(2).toInt(),
                    bankAccountNumber = csvRecord.get(4),
                    amount = csvRecord.get(5).toBigDecimal(),
                    currencyType = csvRecord.get(6),
                    utr = csvRecord.get(9),
                    remarks = csvRecord.get(10),
                    accCode = csvRecord.get(11).toInt(),
                    accMode = AccMode.AR,
                    signFlag = -1,
                    orgSerialId = 0
                )
            )
            recordCount++
        }


        return paymentList
    }
}
