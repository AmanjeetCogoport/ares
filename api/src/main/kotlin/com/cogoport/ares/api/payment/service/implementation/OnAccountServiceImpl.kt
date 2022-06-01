package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.Validations
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.s3.client.S3Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.UUID
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

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updatePayment(receivableRequest: Payment, accountUtilization: AccountUtilization, payment: com.cogoport.ares.api.payment.entity.Payment?): Payment? {
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

    fun updateAccountUtilizationEntry(accountUtilization: AccountUtilization, receivableRequest: Payment): AccountUtilization {
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
        accountUtilization.accMode = receivableRequest.accMode!!
        accountUtilization.signFlag = receivableRequest.signFlag
        accountUtilization.amountCurr = receivableRequest.amount
        accountUtilization.amountLoc = receivableRequest.ledAmount
        accountUtilization.dueDate = receivableRequest.transactionDate!!
        accountUtilization.transactionDate = receivableRequest.transactionDate!!
        return accountUtilization
    }

//    @Transactional(rollbackOn = [Exception::class, AresException::class])
//    override suspend fun createBulkOnAccountPayments(fileUrl: String): BulkPaymentResponse? {
//
//        val docData = s3Client.download(fileUrl)
//        val bufferedReader = FileReader(docData)
//
//        val csvParser = CSVParser(
//            bufferedReader,
//            CSVFormat.DEFAULT
//                .withFirstRecordAsHeader()
//                .withIgnoreHeaderCase()
//                .withTrim()
//                .withDelimiter('|')
//        )
//
//        // Validate excel List before passing it for insertion
//
//        var (paymentModelList: MutableList<Payment>, fileStats, fileUrl) = readAndValidateCSV(csvParser)
//        val (totalCount, successCount) = fileStats
//
//        var paymentEntityList = arrayListOf<com.cogoport.ares.api.payment.entity.Payment>()
////        for (payment in paymentModelList) {
////
////            paymentEntityList.add(paymentConverter.convertToEntity(payment))
////            var savePayment = paymentRepository.save(paymentConverter.convertToEntity(payment))
////            var accUtilizationModel: AccUtilizationRequest =
////                accUtilizationToPaymentConverter.convertEntityToModel(savePayment)
////
////            var paymentModel = paymentConverter.convertToModel(savePayment)
////            Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX,savePayment.id.toString(),paymentModel)
////            accUtilizationModel.accType = "PAY"
////            accUtilizationModel.currencyPayment = 0.toBigDecimal()
////            accUtilizationModel.ledgerPayment = 0.toBigDecimal()
////            accUtilizationModel.ledgerAmount = 0.toBigDecimal()
////            accountUtilizationRepository.save(accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel))
////        }
//
////        var response = BulkPaymentResponse(
////            errorFileUrl = fileUrl,
////            totalCount = totalCount,
////            successCount = successCount
////        )
//        return null
//    }

//    private fun readAndValidateCSV(csvParser: CSVParser): Triple<MutableList<Payment>, Pair<Int, Int>, String> {
//        var paymentList = mutableListOf<Payment>()
//        var hasErrors = false
//        var errorCount = 0
//        var recordCount = 1
//        // Csv Printer Declaration
//        val writer = Files.newBufferedWriter(Paths.get("/Users/mohitmogera/Documents/invoice_errors.csv"))
//        val csvPrinter = CSVPrinter(
//            writer,
//            CSVFormat.DEFAULT
//                .withHeader("customer_name", "customer_id", "entity_type", "cogo_bank", "account_number", "amount", "currency_type", "uploaded_date", "uploaded_by", "UTR", "remarks", "acc_code", "errors")
//                .withDelimiter('|')
//        )
//
//        for (csvRecord in csvParser) {
//            var errors = StringBuilder()
//
//            // Csv Field Validations
//            if (!Validations.checkForCustomerName(csvRecord.get(0))) errors.append(" Invalid Customer Name,"); hasErrors = true
//            if (!Validations.validateUTR(csvRecord.get(9))) errors.append(" Invalid UTR No,"); hasErrors = true
//            if (!Validations.checkForCurrency(csvRecord.get(6))) errors.append(" Invalid Currency,"); hasErrors = true
//            if (!Validations.checkForNumeral(csvRecord.get(5))) errors.append(" Invalid Amount"); hasErrors = true
//
//            var paymentObj = Payment(
//                customerName = csvRecord.get(0),
//                customerId = UUID.fromString(csvRecord.get(1)),
//                entityType = if (!csvRecord.get(2).isNullOrEmpty()) csvRecord.get(2).toInt() else 0,
//                bankAccountNumber = csvRecord.get(4),
//                amount = if (!csvRecord.get(5).isNullOrEmpty()) csvRecord.get(5).toBigDecimal() else 0.toBigDecimal(),
//                currencyType = csvRecord.get(6),
//                utr = csvRecord.get(9),
//                remarks = csvRecord.get(10),
//                accCode = if (!csvRecord.get(11).isNullOrEmpty()) csvRecord.get(11).toInt() else 0,
//                accMode = AccMode.AR,
//                signFlag = -1,
//                orgSerialId = 0
//            )
//
//            // Write records in CSV
//            if (hasErrors) {
//                csvPrinter.printRecord(
//                    csvRecord.get(0), csvRecord.get(1), csvRecord.get(2), csvRecord.get(3), csvRecord.get(4), csvRecord.get(5),
//                    csvRecord.get(6), csvRecord.get(7), csvRecord.get(8), csvRecord.get(8), csvRecord.get(10), csvRecord.get(11), errors
//                )
//                errorCount ++
//            } else {
//                if (paymentObj != null) {
//                    paymentList.add(paymentObj)
//                }
//            }
//            hasErrors = false
//            recordCount++
//        }
//        csvPrinter.flush()
//        csvPrinter.close()
//
//        var s3Response: Any? = null
//        // Upload the paymentFailList file on cloud and return the link.
//        if (errorCount > 0) {
//            val file = File("/Users/mohitmogera/Documents/invoice_errors.csv")
//            s3Response = s3Client.upload("business-finance-test", "invoice_errors.csv", file)
//        }
//
//        var fileStats = Pair(recordCount, recordCount - errorCount)
//
//        return Triple(paymentList, fileStats, s3Response.toString())
//    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    override suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse {

        var paymentEntityList = arrayListOf<com.cogoport.ares.api.payment.entity.Payment>()
        for (payment in bulkPayment) {
            paymentEntityList.add(paymentConverter.convertToEntity(payment))
            var savePayment = paymentRepository.save(paymentConverter.convertToEntity(payment))
            var accUtilizationModel: AccUtilizationRequest =
                accUtilizationToPaymentConverter.convertEntityToModel(savePayment)

            var paymentModel = paymentConverter.convertToModel(savePayment)
            Client.addDocument(AresConstants.ON_ACCOUNT_PAYMENT_INDEX, savePayment.id.toString(), paymentModel)
            accUtilizationModel.accType = "PAY"
            accUtilizationModel.currencyPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerPayment = 0.toBigDecimal()
            accUtilizationModel.ledgerAmount = 0.toBigDecimal()
            accountUtilizationRepository.save(accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel))
        }

        return BulkPaymentResponse(recordsInserted = bulkPayment.size)
    }
}
