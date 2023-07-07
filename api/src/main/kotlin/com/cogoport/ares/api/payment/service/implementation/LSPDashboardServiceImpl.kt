package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.gateway.ExchangeClient
import com.cogoport.ares.api.payment.model.requests.SupplierPaymentStatsRequest
import com.cogoport.ares.api.payment.model.requests.SupplierReceivableRequest
import com.cogoport.ares.api.payment.model.response.AmountAndCount
import com.cogoport.ares.api.payment.model.response.DocumentResponse
import com.cogoport.ares.api.payment.model.response.SupplierReceivables
import com.cogoport.ares.api.payment.model.response.SupplierStatistics
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.LSPDashboardService
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.utils.ExcelUtils
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ExchangeRateRequest
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.request.LSPLedgerRequest
import com.cogoport.ares.model.payment.response.LSPLedgerResponse
import com.cogoport.ares.model.payment.response.LedgerExcelResponse
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import kotlin.collections.ArrayList

@Singleton
class LSPDashboardServiceImpl : LSPDashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    @Inject
    lateinit var accUtilRepo: AccountUtilizationRepo

    @Inject
    private lateinit var documentMapper: DocumentMapper

    @Inject
    private lateinit var paymentRepository: PaymentRepository

    @Inject
    private lateinit var s3Client: S3Client

    @Inject
    private lateinit var exchangeClient: ExchangeClient

    @Value("\${aws.s3.bucket}")
    private lateinit var s3Bucket: String

    override suspend fun getReceivableStatsForSupplier(request: SupplierReceivableRequest): SupplierReceivables {
        val accountTypes = listOf(AccountType.PINV.name, AccountType.PREIMB.name, AccountType.PCN.name)
        val documents = accUtilRepo.getDocumentsForLSP(request.orgId, request.entityCode, null, null, accountTypes)
        if (documents.isEmpty()) {
            return SupplierReceivables(
                currency = request.currency,
                totalReceivables = AmountAndCount(BigDecimal.ZERO, 0),
                unpaidReceivables = AmountAndCount(BigDecimal.ZERO, 0),
                partialPaidReceivables = AmountAndCount(BigDecimal.ZERO, 0)
            )
        }
        var transactionDates = mutableListOf<Date>()
        var currencyList = mutableListOf<String>()
        var totalReceivableAmount = BigDecimal.ZERO
        var unpaidReceivableAmount = BigDecimal.ZERO
        var partialPaidReceivableAmount = BigDecimal.ZERO

        val unpaidDocuments: List<DocumentResponse?> = documents.filter { it?.payCurr == BigDecimal.ZERO }
        val partialPaidDocuments: List<DocumentResponse?> = documents.filter { (it?.amountCurr!! - it.payCurr) > BigDecimal.ZERO && it.payCurr != BigDecimal.ZERO }

        if (request.currency != documents[0]?.ledCurrency) {
            documents.forEach { doc ->
                if (doc?.currency != request.currency) {
                    transactionDates.add(doc?.transactionDate!!)
                    currencyList.add(doc.currency)
                }
            }
            currencyList = ArrayList(currencyList.distinct())
            transactionDates = ArrayList(transactionDates.distinct())

            val exchangeRateResponse = exchangeClient.getExchangeRates(ExchangeRateRequest(currencyList, listOf(request.currency), transactionDates.map { SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it) }))
            documents.forEach { doc ->
                val exchangeRate = exchangeRateResponse.filter { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc?.transactionDate) && it.fromCurrency == doc?.currency }.firstOrNull()?.exchangeRate
                totalReceivableAmount += exchangeRate
                    ?: (BigDecimal.ONE.multiply((doc?.amountCurr!! - doc.payCurr)) * BigDecimal.valueOf(doc.signFlag.toLong(), 0))
            }

            if (unpaidDocuments.isNotEmpty()) {
                unpaidDocuments.forEach { doc ->
                    val exchangeRate = exchangeRateResponse.filter { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc?.transactionDate) && it.fromCurrency == doc?.currency }.firstOrNull()?.exchangeRate
                    unpaidReceivableAmount += exchangeRate
                        ?: (BigDecimal.ONE.multiply((doc?.amountCurr!! - doc.payCurr)) * BigDecimal.valueOf(doc.signFlag.toLong(), 0))
                }
            }
            if (partialPaidDocuments.isNotEmpty()) {
                partialPaidDocuments.forEach { doc ->
                    val exchangeRate = exchangeRateResponse.filter { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc?.transactionDate) && it.fromCurrency == doc?.currency }.firstOrNull()?.exchangeRate
                    partialPaidReceivableAmount += exchangeRate
                        ?: (BigDecimal.ONE.multiply((doc?.amountCurr!! - doc.payCurr)) * BigDecimal.valueOf(doc.signFlag.toLong(), 0))
                }
            }
        } else {
            documents.forEach {
                totalReceivableAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
            }

            if (unpaidDocuments.isNotEmpty()) {
                unpaidDocuments.forEach {
                    unpaidReceivableAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
                }
            }
            if (partialPaidDocuments.isNotEmpty()) {
                partialPaidDocuments.forEach {
                    partialPaidReceivableAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
                }
            }
        }

        return SupplierReceivables(
            totalReceivables = AmountAndCount(totalReceivableAmount, documents.size),
            unpaidReceivables = AmountAndCount(unpaidReceivableAmount, unpaidDocuments.size),
            partialPaidReceivables = AmountAndCount(partialPaidReceivableAmount, partialPaidDocuments.size),
            currency = request.currency
        )
    }

    override suspend fun getPaymentStatsForSupplier(request: SupplierPaymentStatsRequest): SupplierStatistics {
        val invoiceDueStats: AmountAndCount
        val onAccountPayment: AmountAndCount
        var invoicesDueAmount = BigDecimal.ZERO
        var onAccountAmount = BigDecimal.ZERO

        val accountTypesForDue = listOf(AccountType.PINV.name, AccountType.PREIMB.name, AccountType.PCN.name)
        val accountTypesForOnAccount = listOf(
            AccountType.PAY.name, AccountType.BANK.name, AccountType.MISC.name,
            AccountType.OPDIV.name, AccountType.INTER.name, AccountType.CONTR.name, AccountType.MTCCV.name
        )
        if (request.endDate.isNullOrEmpty()) {
            val localDate = LocalDate.now()
            val lastMonth = localDate.minusMonths(1)
            when (request.timePeriod) {
                "seven" -> {
                    request.endDate = localDate.toString()
                    request.startDate = (LocalDate.now().minusDays(7)).toString()
                }
                "month" -> {
                    request.endDate = localDate.toString()
                    request.startDate = (LocalDate.now().minusDays(30)).toString()
                }
                "thirty" -> {
                    request.startDate = lastMonth.withDayOfMonth(1).toString()
                    request.endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toString()
                }
                "threeMonth" -> {
                    val last3Months = localDate.minusMonths(3)
                    request.startDate = last3Months.withDayOfMonth(1).toString()
                    request.endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toString()
                }
                "sixMonth" -> {
                    val last6Months = localDate.minusMonths(6)
                    request.startDate = last6Months.withDayOfMonth(1).toString()
                    request.endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toString()
                }
                else -> {
                    request.startDate = LocalDate.now().toString()
                    request.endDate = LocalDate.now().toString()
                }
            }
        }
        val documentsForOnAccount = accUtilRepo.getDocumentsForLSP(request.orgId, request.entityCode, request.startDate, request.endDate, accountTypesForOnAccount)
        val documentsForDue = accUtilRepo.getDocumentsForLSP(request.orgId, request.entityCode, request.startDate, request.endDate, accountTypesForDue)
        if (documentsForDue.isEmpty()) {
            invoiceDueStats = AmountAndCount(BigDecimal.ZERO, 0)
        } else {
            var transactionDates = arrayListOf<Date>()
            var currencyList = arrayListOf<String>()

            if (request.currency != documentsForDue[0]?.ledCurrency) {
                documentsForDue.forEach { doc ->
                    if (doc?.currency != request.currency) {
                        transactionDates.add(doc?.transactionDate!!)
                        currencyList.add(doc.currency)
                    }
                }
                transactionDates = ArrayList(transactionDates.distinct())
                currencyList = ArrayList(currencyList.distinct())
                val exchangeRateResponse = exchangeClient.getExchangeRates(ExchangeRateRequest(currencyList, arrayListOf(request.currency), transactionDates.map { SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it) }))
                documentsForDue.forEach { doc ->
                    val exchangeRate = exchangeRateResponse.filter { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc?.transactionDate) && it.fromCurrency == doc?.currency }.firstOrNull()?.exchangeRate
                    invoicesDueAmount += exchangeRate
                        ?: (BigDecimal.ONE.multiply((doc?.amountCurr!! - doc.payCurr)) * BigDecimal.valueOf(doc.signFlag.toLong(), 0))
                }
            } else {
                documentsForDue.forEach {
                    invoicesDueAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
                }
            }
            invoiceDueStats = AmountAndCount(invoicesDueAmount, documentsForDue.size)
        }

        if (documentsForOnAccount.isEmpty()) {
            onAccountPayment = AmountAndCount(BigDecimal.ZERO, 0)
        } else {
            var transactionDates = mutableListOf<Date>()
            var currencyList = mutableListOf<String>()

            if (request.currency != documentsForDue[0]?.ledCurrency) {
                documentsForOnAccount.forEach { doc ->
                    if (doc?.currency != request.currency) {
                        transactionDates.add(doc?.transactionDate!!)
                        currencyList.add(doc.currency)
                    }
                }
                transactionDates = ArrayList(transactionDates.distinct())
                currencyList = ArrayList(currencyList.distinct())
                val exchangeRateResponse = exchangeClient.getExchangeRates(ExchangeRateRequest(currencyList, listOf(request.currency), transactionDates.map { SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it) }))
                documentsForOnAccount.forEach { doc ->
                    val exchangeRate = exchangeRateResponse.filter { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc?.transactionDate) && it.fromCurrency == doc?.currency }.firstOrNull()?.exchangeRate
                    onAccountAmount += exchangeRate ?: BigDecimal.ONE.multiply((doc?.amountCurr!! - doc.payCurr) * BigDecimal.valueOf(doc.signFlag.toLong(), 0))
                }
            } else {
                documentsForOnAccount.forEach {
                    onAccountAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
                }
            }
            onAccountPayment = AmountAndCount(onAccountAmount, documentsForOnAccount.size)
        }

        return SupplierStatistics(
            invoicesDue = invoiceDueStats,
            onAccountPayment = onAccountPayment,
            currency = request.currency
        )
    }

    override suspend fun getLSPLedger(request: LSPLedgerRequest): LSPLedgerResponse {
        val accTypes = listOf(
            AccountType.PAY.name, AccountType.PREIMB.name, AccountType.PINV.name, AccountType.MISC.name, AccountType.PCN.name, AccountType.MTC.name,
            AccountType.OPDIV.name, AccountType.BANK.name, AccountType.INTER.name, AccountType.CONTR.name, AccountType.MTCCV.name, AccountType.ROFF.name
        )
        val month = AresConstants.MONTH[request.month]
        val ledgerDocumentsByPagination = accUtilRepo.getLedgerForLSP(request.orgId, request.entityCode, request.year, month!!, accTypes, request.page, request.pageLimit)
        if (ledgerDocumentsByPagination.isNullOrEmpty()) {
            return LSPLedgerResponse(
                openingBalance = BigDecimal.ZERO,
                closingBalance = BigDecimal.ZERO,
                ledgerCurrency = "",
                ledgerDocuments = emptyList()
            )
        }

        val allLedgerDocs = accUtilRepo.getLedgerForLSP(request.orgId, request.entityCode, request.year, month, accTypes, null, null)
        val openingBalance = allLedgerDocs?.get(0)?.debit?.minus(allLedgerDocs[0].credit) ?: BigDecimal.ZERO
        val closingBalance = allLedgerDocs?.sumOf { it.debit.minus(it.credit) } ?: BigDecimal.ZERO

        val totalCount = accUtilRepo.getLedgerForLSPCount(request.orgId, request.entityCode, request.year, month, accTypes)
        val description = mapOf(
            "PAY" to "Payment", "PCN" to "Credit note", "PREIMB" to "Reimbursement", "PINV" to "Invoice", "MISC" to "Miscellaneous",
            "ROFF" to "Round Off JV", "BANK" to "Bank"
        )

        val ledgerDocs = documentMapper.convertLedgerDetailsToLSPLedgerDocuments(ledgerDocumentsByPagination)
        val documentNos = ledgerDocs.filter { doc -> doc.type == AccountType.PAY.name }.map { it.documentNo }
        val nonPaymentDocumentNos = ledgerDocs.filter { doc -> doc.type in listOf(AccountType.PINV.name, AccountType.PCN.name, AccountType.PREIMB.name) }.map { it.documentNo }
        val sids = unifiedDBRepo.getJobNumbersByDocumentNos(nonPaymentDocumentNos)

        val documentUTRNoMap = mutableMapOf<Long, String?>()
        val documentSIDMap = mutableMapOf<Long, String?>()
        val transRefNos = paymentRepository.findTransRefNumByPaymentNums(documentNos, AccMode.AP.name, request.orgId, AccountType.PAY.name)
        transRefNos.forEach {
            documentUTRNoMap[it.paymentNum] = it.transRefNumber
        }

        sids.forEach {
            documentSIDMap[it.id] = it.jobNumber
        }

        var balance = BigDecimal.ZERO
        ledgerDocs.forEach {
            it.type = description[it.type] ?: it.type
            it.documentValue = documentUTRNoMap[it.documentNo] ?: it.documentValue
            it.shipmentId = documentSIDMap[it.documentNo] ?: "NA"
            balance += (it.debit - it.credit)
            it.balance = balance
            it.debitBalance = if (it.balance!! > BigDecimal.ZERO) { it.balance } else {
                BigDecimal.ZERO
            }
            it.creditBalance = if (it.balance!! < BigDecimal.ZERO) { it.balance!!.negate() } else {
                BigDecimal.ZERO
            }
        }

        val totalPages = Utilities.getTotalPages(totalCount, request.pageLimit!!)
        return LSPLedgerResponse(
            ledgerCurrency = ledgerDocs[0].ledgerCurrency,
            openingBalance = openingBalance,
            closingBalance = closingBalance,
            ledgerDocuments = ledgerDocs,
            totalPages = totalPages,
            page = request.page,
            totalRecords = totalCount
        )
    }

    override suspend fun downloadLSPLedger(request: LSPLedgerRequest): String? {
        val accTypes = listOf(
            AccountType.PAY.name, AccountType.PREIMB.name, AccountType.PINV.name, AccountType.MISC.name, AccountType.PCN.name, AccountType.MTC.name,
            AccountType.OPDIV.name, AccountType.BANK.name, AccountType.INTER.name, AccountType.CONTR.name, AccountType.MTCCV.name, AccountType.ROFF.name
        )
        val month = AresConstants.MONTH[request.month]
        val ledgerDocuments = accUtilRepo.getLedgerForLSP(request.orgId, request.entityCode, request.year, month!!, accTypes, null, null)
        if (ledgerDocuments.isNullOrEmpty()) {
            return null
        }
        val description = mapOf(
            "PAY" to "Payment", "PCN" to "Credit note", "PREIMB" to "Reimbursement", "PINV" to "Invoice", "MISC" to "Miscellaneous",
            "ROFF" to "Round Off JV", "BANK" to "Bank"
        )

        val ledgerDocs = documentMapper.convertLedgerDetailsToLSPLedgerDocuments(ledgerDocuments)
        val documentNos = ledgerDocs.filter { doc -> doc.type == AccountType.PAY.name }.map { it.documentNo }
        val nonPaymentDocumentNos = ledgerDocs.filter { doc -> doc.type in listOf(AccountType.PINV.name, AccountType.PCN.name, AccountType.PREIMB.name) }.map { it.documentNo }

        val documentUTRNoMap = mutableMapOf<Long, String?>()
        val documentSIDMap = mutableMapOf<Long, String?>()
        val transRefNos = paymentRepository.findTransRefNumByPaymentNums(documentNos, AccMode.AP.name, request.orgId, AccountType.PAY.name)
        transRefNos.forEach {
            documentUTRNoMap[it.paymentNum] = it.transRefNumber
        }

        val sids = unifiedDBRepo.getJobNumbersByDocumentNos(nonPaymentDocumentNos)

        sids.forEach {
            documentSIDMap[it.id] = it.jobNumber
        }

        val ledgerExcelList = mutableListOf<LedgerExcelResponse>()
        var balance = BigDecimal.ZERO
        ledgerDocs.forEach {
            balance += (it.debit - it.credit)
            it.balance = balance
            it.debitBalance = if (it.balance!! > BigDecimal.ZERO) { it.balance } else {
                BigDecimal.ZERO
            }
            it.creditBalance = if (it.balance!! < BigDecimal.ZERO) { it.balance!!.negate() } else {
                BigDecimal.ZERO
            }
            val ledgerExcelResponse = LedgerExcelResponse(
                transactionDate = it.transactionDate.toString(),
                ledgerCurrency = it.ledgerCurrency,
                shipmentId = documentSIDMap[it.documentNo] ?: "NA",
                type = description[it.type] ?: it.type,
                documentValue = documentUTRNoMap[it.documentNo] ?: it.documentValue,
                balance = it.balance,
                debitBalance = it.debitBalance,
                creditBalance = it.creditBalance,
                debit = it.debit,
                credit = it.credit
            )
            ledgerExcelList.add(ledgerExcelResponse)
        }

        val excelName = "Ledger_" + request.month + "_" + request.year + "_" + ledgerDocs.size
        val file = ExcelUtils.writeIntoExcel(ledgerExcelList, excelName, "Ledger Sheet")
        val url = s3Client.upload(s3Bucket, "$excelName.xlsx", file).toString()
        return url
    }
}
