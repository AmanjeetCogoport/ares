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
import com.cogoport.ares.model.common.ExchangeRateResponseByDate
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.request.LSPLedgerRequest
import com.cogoport.ares.model.payment.response.LSPLedgerResponse
import com.cogoport.ares.model.payment.response.LedgerDetails
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

    @Value("\${lsp.coming_soon_banner.enabled}")
    private var isComingSoonEnabled: Boolean = true

    override suspend fun getReceivableStatsForSupplier(request: SupplierReceivableRequest): SupplierReceivables {
        val accountTypes = listOf(AccountType.PINV.name, AccountType.PREIMB.name, AccountType.PCN.name)
        val documents = accUtilRepo.getDocumentsForLSP(request.orgId, request.entityCode, null, null, accountTypes)
        if (documents.isEmpty()) {
            return SupplierReceivables(
                currency = request.currency,
                totalReceivables = AmountAndCount(BigDecimal.ZERO, 0),
                unpaidReceivables = AmountAndCount(BigDecimal.ZERO, 0),
                partialPaidReceivables = AmountAndCount(BigDecimal.ZERO, 0),
                isComingSoonEnabled = isComingSoonEnabled
            )
        }

        var totalReceivableAmount = BigDecimal.ZERO
        var unpaidReceivableAmount = BigDecimal.ZERO
        var partialPaidReceivableAmount = BigDecimal.ZERO

        val unpaidDocuments: List<DocumentResponse?> = documents.filter { it?.payCurr == BigDecimal.ZERO }
        val partialPaidDocuments: List<DocumentResponse?> = documents.filter { (it?.amountCurr!! - it.payCurr) > BigDecimal.ZERO && it.payCurr != BigDecimal.ZERO }

        if (request.currency != documents[0]?.ledCurrency) {
            val exchangeRateResponse = getExchangeRate(documents, request.currency)
            documents.forEach { doc ->
                val exchangeRate = getExchangeRateByDate(doc?.transactionDate!!, doc.currency, exchangeRateResponse)
                totalReceivableAmount += ((doc.amountCurr - doc.payCurr) * BigDecimal.valueOf(doc.signFlag.toLong(), 0)) * exchangeRate
            }

            if (unpaidDocuments.isNotEmpty()) {
                unpaidDocuments.forEach { doc ->
                    val exchangeRate = getExchangeRateByDate(doc?.transactionDate!!, doc.currency, exchangeRateResponse)
                    unpaidReceivableAmount += ((doc.amountCurr - doc.payCurr) * BigDecimal.valueOf(doc.signFlag.toLong(), 0)) * exchangeRate
                }
            }
            if (partialPaidDocuments.isNotEmpty()) {
                partialPaidDocuments.forEach { doc ->
                    val exchangeRate = getExchangeRateByDate(doc?.transactionDate!!, doc.currency, exchangeRateResponse)
                    partialPaidReceivableAmount += ((doc.amountCurr - doc.payCurr) * BigDecimal.valueOf(doc.signFlag.toLong(), 0)) * exchangeRate
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
            currency = request.currency,
            isComingSoonEnabled = isComingSoonEnabled
        )
    }

    override suspend fun getPaymentStatsForSupplier(request: SupplierPaymentStatsRequest): SupplierStatistics {
        var invoiceDueStats: AmountAndCount
        val onAccountPayment: AmountAndCount
        var invoicesDueAmount = BigDecimal.ZERO
        var onAccountAmount = BigDecimal.ZERO

        val accountTypesForDue = listOf(AccountType.PINV.name, AccountType.PREIMB.name, AccountType.PCN.name)
        val accountTypesForOnAccount = listOf(
            AccountType.PAY.name, AccountType.BANK.name, AccountType.MISC.name
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
        } else if (request.currency != documentsForDue[0]?.ledCurrency) {
            val exchangeRateResponse = getExchangeRate(documentsForDue, request.currency)
            documentsForDue.forEach { doc ->
                val exchangeRate = getExchangeRateByDate(doc?.transactionDate!!, doc.currency, exchangeRateResponse)
                invoicesDueAmount += ((doc.amountCurr - doc.payCurr) * BigDecimal.valueOf(doc.signFlag.toLong(), 0)) * exchangeRate
            }
            invoiceDueStats = AmountAndCount(invoicesDueAmount, documentsForDue.size)
        } else {
            documentsForDue.forEach {
                invoicesDueAmount += (it?.amountLoc!! - it.payLoc) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
            }
            invoiceDueStats = AmountAndCount(invoicesDueAmount, documentsForDue.size)
        }

        if (documentsForOnAccount.isEmpty()) {
            onAccountPayment = AmountAndCount(BigDecimal.ZERO, 0)
        } else if (request.currency != documentsForOnAccount[0]?.ledCurrency) {
            val exchangeRateResponse = getExchangeRate(documentsForOnAccount, request.currency)
            documentsForOnAccount.forEach { doc ->
                val exchangeRate = getExchangeRateByDate(doc?.transactionDate!!, doc.currency, exchangeRateResponse)
                onAccountAmount += (doc.amountCurr * BigDecimal.valueOf(doc.signFlag.toLong(), 0)) * exchangeRate
            }
            onAccountPayment = AmountAndCount(onAccountAmount, documentsForOnAccount.size)
        } else {
            documentsForOnAccount.forEach {
                onAccountAmount += (it?.amountLoc!!) * BigDecimal.valueOf(it.signFlag.toLong(), 0)
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
            AccountType.PAY.name, AccountType.PREIMB.name, AccountType.PINV.name, AccountType.MISC.name, AccountType.PCN.name, AccountType.BANK.name
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

        var exchangeRateResponse = mutableListOf<ExchangeRateResponseByDate>()
        var openingBalance = BigDecimal.ZERO
        var closingBalance = BigDecimal.ZERO
        val documentNoBalanceMap = hashMapOf<Long, BigDecimal>()
        val allLedgerDocs = accUtilRepo.getLedgerForLSP(request.orgId, request.entityCode, request.year, month, accTypes, null, null)
        if (request.currency != allLedgerDocs!![0].ledgerCurrency) {
            exchangeRateResponse = getExchangeRateForLedgerDocs(allLedgerDocs, request.currency)
            val exchangeRateForOpeningBalance = getExchangeRateByDate(allLedgerDocs.get(0).transactionDate, allLedgerDocs.get(0).ledgerCurrency, exchangeRateResponse)
            openingBalance = exchangeRateForOpeningBalance * allLedgerDocs.get(0).debit.plus(allLedgerDocs[0].credit)
            allLedgerDocs.forEach { doc ->
                val exchangeRate = getExchangeRateByDate(doc.transactionDate, doc.ledgerCurrency, exchangeRateResponse)
                closingBalance += exchangeRate * (doc.debit.plus(doc.credit))
                documentNoBalanceMap[doc.documentNo] = closingBalance
            }
        } else {
            openingBalance = allLedgerDocs.get(0).debit.plus(allLedgerDocs[0].credit)
            allLedgerDocs.forEach { doc ->
                closingBalance += (doc.debit.plus(doc.credit))
                documentNoBalanceMap[doc.documentNo] = closingBalance
            }
        }

        val totalCount = accUtilRepo.getLedgerForLSPCount(request.orgId, request.entityCode, request.year, month, accTypes)
        val description = mapOf(
            "PAY" to "Payment", "PCN" to "Credit note", "PREIMB" to "Reimbursement", "PINV" to "Invoice", "MISC" to "Miscellaneous", "BANK" to "Bank"
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

        ledgerDocs.forEach { doc ->
            val exchangeRate = getExchangeRateByDate(doc.transactionDate, doc.ledgerCurrency, exchangeRateResponse)
            doc.type = description[doc.type] ?: doc.type
            doc.ledgerCurrency = request.currency
            doc.documentValue = documentUTRNoMap[doc.documentNo] ?: doc.documentValue
            doc.shipmentId = documentSIDMap[doc.documentNo] ?: "NA"
            doc.debit = exchangeRate * doc.debit
            doc.credit = exchangeRate * doc.credit
            doc.balance = documentNoBalanceMap[doc.documentNo]
            doc.debitBalance = if (doc.balance!! > BigDecimal.ZERO) { doc.balance } else {
                BigDecimal.ZERO
            }
            doc.creditBalance = if (doc.balance!! < BigDecimal.ZERO) { doc.balance!!.negate() } else {
                BigDecimal.ZERO
            }
        }
        val totalPages = Utilities.getTotalPages(totalCount, request.pageLimit!!)
        return LSPLedgerResponse(
            ledgerCurrency = request.currency,
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
            AccountType.PAY.name, AccountType.PREIMB.name, AccountType.PINV.name, AccountType.MISC.name, AccountType.PCN.name, AccountType.BANK.name
        )
        val month = AresConstants.MONTH[request.month]
        val ledgerDocuments = accUtilRepo.getLedgerForLSP(request.orgId, request.entityCode, request.year, month!!, accTypes, null, null)
        if (ledgerDocuments.isNullOrEmpty()) {
            return null
        }
        if (request.currency != ledgerDocuments[0].ledgerCurrency) {
            val exchangeRateResponse = getExchangeRateForLedgerDocs(ledgerDocuments, request.currency)
            ledgerDocuments.forEach { doc ->
                val exchangeRate = getExchangeRateByDate(doc.transactionDate, doc.ledgerCurrency, exchangeRateResponse)
                doc.debit = doc.debit * exchangeRate
                doc.credit = doc.credit * exchangeRate
            }
        }
        val description = mapOf(
            "PAY" to "Payment", "PCN" to "Credit note", "PREIMB" to "Reimbursement", "PINV" to "Invoice", "MISC" to "Miscellaneous", "BANK" to "Bank"
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
            balance += (it.debit + it.credit)
            it.balance = balance
            it.debitBalance = if (it.balance!! > BigDecimal.ZERO) { it.balance } else {
                BigDecimal.ZERO
            }
            it.creditBalance = if (it.balance!! < BigDecimal.ZERO) { it.balance!!.negate() } else {
                BigDecimal.ZERO
            }
            val ledgerExcelResponse = LedgerExcelResponse(
                transactionDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it.transactionDate),
                ledgerCurrency = request.currency,
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

    private suspend fun getExchangeRate(documents: List<DocumentResponse?>, toCurrency: String): MutableList<ExchangeRateResponseByDate>? {
        var transactionDates = mutableListOf<Date>()
        var currencyList = mutableListOf<String>()
        documents.forEach { doc ->
            if (doc?.currency != toCurrency) {
                transactionDates.add(doc?.transactionDate!!)
                currencyList.add(doc.currency)
            }
        }
        currencyList = ArrayList(currencyList.distinct())
        transactionDates = ArrayList(transactionDates.distinct())
        return if (!currencyList.isNullOrEmpty()) {
            exchangeClient.getExchangeRates(
                ExchangeRateRequest(
                    currencyList, listOf(toCurrency),
                    transactionDates.map { SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it) }
                )
            )
        } else {
            null
        }
    }

    private suspend fun getExchangeRateForLedgerDocs(ledgerDocs: List<LedgerDetails>?, toCurrency: String): MutableList<ExchangeRateResponseByDate> {
        var transactionDates = mutableListOf<Date>()
        ledgerDocs?.forEach { doc ->
            transactionDates.add(doc.transactionDate)
        }
        transactionDates = ArrayList(transactionDates.distinct())
        return exchangeClient.getExchangeRates(
            ExchangeRateRequest(
                listOf(ledgerDocs!![0].ledgerCurrency), listOf(toCurrency),
                transactionDates.map { SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it) }
            )
        )
    }

    private fun getExchangeRateByDate(transactionDate: Date, currency: String, exchangeRateResponse: MutableList<ExchangeRateResponseByDate>?): BigDecimal {
        return exchangeRateResponse?.firstOrNull { it.exchangeRateDate == SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(transactionDate) && it.fromCurrency == currency }?.exchangeRate
            ?: BigDecimal.ONE
    }
}
