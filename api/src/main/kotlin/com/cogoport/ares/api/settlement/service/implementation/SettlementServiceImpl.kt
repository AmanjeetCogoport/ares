package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.common.models.TdsStylesResponse
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.OrgSummaryMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.utils.Hashids
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.Operator
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.TdsStyle
import com.cogoport.ares.model.settlement.event.InvoiceBalance
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.plutus.client.PlutusClient
import com.cogoport.plutus.model.receivables.SidResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil
import kotlin.math.roundToInt

@Singleton
open class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var historyDocumentConverter: HistoryDocumentMapper

    @Inject
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var documentConverter: DocumentMapper

    @Inject
    lateinit var orgSummaryConverter: OrgSummaryMapper

    @Inject
    lateinit var cogoClient: AuthClient

    @Inject
    lateinit var plutusClient: PlutusClient

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    private lateinit var hashId: Hashids

    /**
     * Get documents for Given Business partner/partners in input request.
     * @param settlementDocumentRequest
     * @return ResponseList
     */
    override suspend fun getDocuments(settlementDocumentRequest: SettlementDocumentRequest): ResponseList<Document>? {
        validateSettlementDocumentInput(settlementDocumentRequest)
        return getDocumentList(settlementDocumentRequest)
    }

    /**
     */
    override suspend fun getTDSDocuments(
        request: TdsSettlementDocumentRequest
    ): ResponseList<Document> {
        validateTdsDocumentInput(request)
        return getTDSDocumentList(request)
    }

    /**
     * Get Account balance of selected Business Partners.
     * @param summaryRequest
     * @return SummaryResponse
     */
    override suspend fun getAccountBalance(summaryRequest: SummaryRequest): SummaryResponse {
        val orgId = getOrgIds(summaryRequest.importerExporterId, summaryRequest.serviceProviderId)
        val amount =
            accountUtilizationRepository.getAccountBalance(
                orgId,
                summaryRequest.entityCode!!,
                summaryRequest.startDate,
                summaryRequest.endDate
            )
        return SummaryResponse(amount)
    }

    /**
     * Get History Document list (Credit Notes and On Account Payments).
     * @param request
     * @return ResponseList<HistoryDocument>
     */
    override suspend fun getHistory(
        request: SettlementHistoryRequest
    ): ResponseList<HistoryDocument?> {
        val accountTypes = stringAccountTypes(request)
        val documents =
            accountUtilizationRepository.getHistoryDocument(
                request.orgId,
                accountTypes,
                request.page,
                request.pageLimit,
                request.startDate,
                request.endDate
            )
        val totalRecords =
            accountUtilizationRepository.countHistoryDocument(
                request.orgId,
                accountTypes,
                request.startDate,
                request.endDate
            )

        val historyDocuments = mutableListOf<HistoryDocument>()
        documents.forEach { doc ->
            historyDocuments.add(historyDocumentConverter.convertToModel(doc))
        }
        historyDocuments.forEach { it.documentNo = hashId.encode(it.documentNo.toLong()) }
        return ResponseList(
            list = historyDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private fun stringAccountTypes(request: SettlementHistoryRequest): MutableList<String> {
        val accountTypes =
            if (request.accountType == "All") {
                mutableListOf(AccountType.PCN.toString(), AccountType.REC.toString())
            } else {
                mutableListOf(request.accountType)
            }
        return accountTypes
    }

    /**
     * Get Total pages from page size and total records
     * @param totalRows
     * @param pageSize
     * @return totalPages
     */
    private fun getTotalPages(totalRows: Long, pageSize: Int): Long {

        return try {
            val totalPageSize = if (pageSize > 0) pageSize else 1
            ceil((totalRows.toFloat() / totalPageSize.toFloat()).toDouble()).roundToInt().toLong()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get Settlement details for input document number
     * @param request
     * @return ResponseList
     */
    override suspend fun getSettlement(
        request: SettlementRequest
    ): ResponseList<com.cogoport.ares.model.settlement.SettledInvoice?> {
        request.documentNo = hashId.decode(request.documentNo)[0].toString()
        val settlementGrouped = getSettlementFromDB(request)
        val paymentIds = mutableListOf(request.documentNo.toLong())
        val payments = getPaymentDataForSettledInvoices(settlementGrouped, paymentIds, request.settlementType)
        val settlements = getSettledInvoices(settlementGrouped, payments)
        // Fetch Sid for invoices
        val invoiceSids = getSidsForInvoices(settlements)
        val settledDocuments = populateSettledDocuments(settlements, request, payments, invoiceSids)

        // Pagination Data
        val totalRecords =
            settlementRepository.countSettlement(request.documentNo.toLong(), request.settlementType)
        settledDocuments.forEach { it.documentNo = hashId.encode(it.documentNo.toLong()) }
        return ResponseList(
            list = settledDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private suspend fun getSidsForInvoices(settlements: MutableList<SettledInvoice>): List<SidResponse>? {
        val invoiceIds = settlements.map { it.destinationId.toString() }
        return if (invoiceIds.isNotEmpty()) plutusClient.getSidsForInvoiceIds(invoiceIds) else null
    }

    private suspend fun getSettledInvoices(
        settlementGrouped: Map<Long?, List<SettledInvoice>>,
        payments: List<PaymentData>
    ): MutableList<SettledInvoice> {
        val settlements = settlementGrouped.map { docList ->
            val settledTds = docList.value.sumOf { doc ->
                if (!doc.tdsCurrency.isNullOrBlank()) {
                    convertPaymentCurrToInvoiceCurr(
                        toCurrency = doc.currency!!,
                        fromCurrency = doc.tdsCurrency,
                        fromLedCurrency = doc.ledCurrency,
                        amount = doc.settledTds,
                        exchangeRate = payments.find { it.documentNo == doc.tdsDocumentNo }?.exchangeRate,
                        exchangeDate = payments.find { it.documentNo == doc.tdsDocumentNo }?.transactionDate!!
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
            docList.value.map { it.settledTds = settledTds }
            docList.value.first()
        }.toMutableList()
        return settlements
    }

    private suspend fun populateSettledDocuments(
        settlements: MutableList<SettledInvoice>,
        request: SettlementRequest,
        payments: List<PaymentData>,
        invoiceSids: List<SidResponse>?
    ): MutableList<com.cogoport.ares.model.settlement.SettledInvoice> {
        val settledDocuments = mutableListOf<com.cogoport.ares.model.settlement.SettledInvoice>()
        settlements.forEach { settlement ->
            when (request.settlementType) {
                SettlementType.REC, SettlementType.PCN -> {
                    // Calculate Settled Amount in Invoice Currency
                    settlement.settledAmount =
                        convertPaymentCurrToInvoiceCurr(
                            toCurrency = settlement.currency!!,
                            fromCurrency = settlement.paymentCurrency,
                            fromLedCurrency = settlement.ledCurrency,
                            amount = settlement.settledAmount!!,
                            exchangeRate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.exchangeRate,
                            exchangeDate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.transactionDate!!
                        )

                    settlement.tds =
                        convertPaymentCurrToInvoiceCurr(
                            toCurrency = settlement.currency,
                            fromCurrency = settlement.paymentCurrency,
                            fromLedCurrency = settlement.ledCurrency,
                            amount = settlement.tds!!,
                            exchangeRate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.exchangeRate,
                            exchangeDate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.transactionDate!!
                        )

                    // Convert To Model
                    val settledDoc = settledInvoiceConverter.convertToModel(settlement)
                    settledDoc.balanceAmount = settledDoc.currentBalance
                    settledDoc.allocationAmount = settledDoc.settledAmount
                    settledDoc.afterTdsAmount -= settledDoc.settledTds!!

                    // Assign Sid
                    settledDoc.sid = invoiceSids?.find { it.invoiceId == settledDoc.documentNo.toLong() }?.jobNumber
                    // Assign Status
                    when (
                        settledDoc.balanceAmount.setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN)
                    ) {
                        BigDecimal.ZERO -> settledDoc.status = DocStatus.PAID.value
                        settledDoc.documentAmount?.setScale(
                            AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN
                        ) -> settledDoc.status = DocStatus.UNPAID.value
                        else -> settledDoc.status = DocStatus.PARTIAL_PAID.value
                    }
                    settledDocuments.add(settledDoc)
                }
                else -> {}
            }
        }
        return settledDocuments
    }

    /**
     * Get Payment date, exchange rate for the settled TDS.
     * @param: settlementGrouped
     * @param: paymentIds
     * @param: settlementType
     */
    private suspend fun getPaymentDataForSettledInvoices(
        settlementGrouped: Map<Long?, List<SettledInvoice>>,
        paymentIds: MutableList<Long>,
        settlementType: SettlementType
    ): List<PaymentData> {
        settlementGrouped.forEach { docList ->
            docList.value.forEach {
                if (it.tdsDocumentNo != null) paymentIds.add(it.tdsDocumentNo)
            }
        }
        val payments = if (settlementType == SettlementType.REC) {
            paymentRepository.findByPaymentNumIn(paymentIds)
        } else {
            accountUtilizationRepository.getPaymentDetails(paymentIds)
        }
        return payments
    }

    /**
     * Get settled document from DB for the payment in request.
     * @param: request
     * @return: Map<Long?, List<SettledInvoice>>
     */
    private suspend fun getSettlementFromDB(request: SettlementRequest): Map<Long?, List<SettledInvoice>> {
        var settlements = mutableListOf<SettledInvoice>()
        when (request.settlementType) {
            SettlementType.REC, SettlementType.PCN -> {
                @Suppress("UNCHECKED_CAST")
                settlements =
                    settlementRepository.findSettlement(
                    request.documentNo.toLong(),
                    request.settlementType,
                    request.page,
                    request.pageLimit
                ) as MutableList<SettledInvoice>
            }
            else -> {}
        }
        // Group Invoices And Calculate settled Tds
        return settlements.groupBy { it.id }
    }

    private suspend fun convertPaymentCurrToInvoiceCurr(
        toCurrency: String,
        fromCurrency: String,
        fromLedCurrency: String,
        exchangeRate: BigDecimal?,
        amount: BigDecimal,
        exchangeDate: Date
    ): BigDecimal {
        return if (toCurrency != fromCurrency) {
            val rate = if (fromLedCurrency == toCurrency) {
                exchangeRate
                    ?: settlementServiceHelper.getExchangeRate(
                        fromCurrency, toCurrency,
                        SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(exchangeDate)
                    )
            } else {
                settlementServiceHelper.getExchangeRate(
                    fromCurrency, toCurrency,
                    SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(exchangeDate)
                )
            }

            Utilities.binaryOperation(
                operandOne = amount,
                operandTwo = rate,
                operation = Operator.MULTIPLY
            )
        } else {
            amount
        }
    }

    private fun calculateTds(rate: BigDecimal, settledTds: BigDecimal, taxableAmount: BigDecimal): BigDecimal {
        val tds =
            taxableAmount * Utilities.binaryOperation(
                rate, 100.toBigDecimal(), Operator.DIVIDE
            ).setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN)
        return if (tds >= settledTds) {
            tds - settledTds
        } else if (settledTds.compareTo(BigDecimal.ZERO) == 0) {
            tds
        } else BigDecimal.ZERO
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param request
     * @return ResponseList
     */
    private suspend fun getDocumentList(
        request: SettlementDocumentRequest
    ): ResponseList<Document> {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val orgId = getOrgIds(request.importerExporterId, request.serviceProviderId)
        val accType = getAccountType(request.importerExporterId, request.serviceProviderId)
        val documentEntity =
            accountUtilizationRepository.getDocumentList(
                request.pageLimit,
                offset,
                accType,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )

        val documentModel = groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        documentModel.forEach { it.documentNo = hashId.encode(it.documentNo.toLong()) }
        val tdsProfiles = orgId.map { getOrgTdsProfile(it) }
        val total =
            accountUtilizationRepository.getDocumentCount(
                accType,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        for (doc in documentModel) {
            val tdsElement = tdsProfiles.find { it?.id == doc.organizationId }
            val rate = getTdsRate(tdsElement)
            doc.tds = calculateTds(
                rate = rate,
                settledTds = doc.settledTds!!,
                taxableAmount = doc.taxableAmount
            )
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
            doc.currentBalance -= doc.tds
            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType))
            doc.status = settlementServiceHelper.getDocumentStatus(
                afterTdsAmount = doc.afterTdsAmount,
                balanceAmount = doc.balanceAmount,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.settledAllocation = BigDecimal.ZERO
            doc.allocationAmount = doc.balanceAmount
            doc.balanceAfterAllocation = BigDecimal.ZERO
        }
        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    private suspend fun getOrgTdsProfile(orgId: UUID): TdsStylesResponse? {
        return try {
            cogoClient.getOrgTdsStyles(orgId.toString()).data
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun groupDocumentList(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>): List<com.cogoport.ares.api.settlement.entity.Document?> {
        return documentEntity.groupBy { it!!.id }.map { docList ->
            val settledTds = docList.value.sumOf { doc ->
                if (doc != null) {
                    calculateSettledTds(doc)
                } else {
                    BigDecimal.ZERO
                }
            }
            docList.value.map { it?.settledTds = settledTds }
            docList.value.first()
        }
    }

    private fun getOrgIds(importerExporterId: UUID?, serviceProviderId: UUID?): List<UUID> {
        val orgId = mutableListOf<UUID>()
        if (importerExporterId != null)
            orgId.add(importerExporterId)
        if (serviceProviderId != null)
            orgId.add(serviceProviderId)
        return orgId
    }

    private fun getAccountType(importerExporterId: UUID?, serviceProviderId: UUID?): List<AccountType> {
        return if (importerExporterId != null && serviceProviderId != null) {
            listOf(AccountType.SINV, AccountType.PINV)
        } else if (importerExporterId != null) {
            listOf(AccountType.SINV, AccountType.REC, AccountType.SCN, AccountType.SDN)
        } else {
            listOf(AccountType.PINV, AccountType.PCN, AccountType.PDN)
        }
    }

    private suspend fun calculateSettledTds(doc: com.cogoport.ares.api.settlement.entity.Document): BigDecimal {
        return if (!doc.tdsCurrency.isNullOrBlank() && (doc.currency != doc.tdsCurrency)) {
            if (doc.ledCurrency == doc.tdsCurrency) {
                getExchangeValue(doc.settledTds, doc.exchangeRate, true)
            } else {
                //  val sourceDoc = accountUtilizationRepository.findRecord(it?.sourceId!!)
                val rate = doc.tdsCurrency?.let { it ->
                    settlementServiceHelper.getExchangeRate(
                        it, doc.currency,
                        SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc.documentDate)
                    )
                } ?: BigDecimal.ZERO
                getExchangeValue(doc.settledTds, rate)
            }
        } else {
            doc.settledTds
        }
    }

    /**
     * Get TDS Rate from styles if present else return default 2%
     * @param tdsProfile
     * @return BigDecimal
     */
    private fun getTdsRate(
        tdsProfile: TdsStylesResponse?
    ): BigDecimal {
        return if (tdsProfile?.tdsDeductionType == "no_deduction") {
            AresConstants.NO_DEDUCTION_RATE.toBigDecimal()
        } else {
            tdsProfile?.tdsDeductionRate ?: AresConstants.DEFAULT_TDS_RATE.toBigDecimal()
        }
    }

    /**
     * Validate input for list of documents
     */
    private fun validateSettlementDocumentInput(request: SettlementDocumentRequest) {
        if (request.entityCode == null) throw AresException(AresError.ERR_1003, "entityCode")
        if (request.importerExporterId == null && request.serviceProviderId == null)
            throw AresException(AresError.ERR_1003, "importerExporterId and serviceProviderId")
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param request
     * @return ResponseList
     */
    private suspend fun getTDSDocumentList(
        request: TdsSettlementDocumentRequest
    ): ResponseList<Document> {
        val offset =
            request.pageLimit?.let {
                (request.page?.let { request.pageLimit?.times(it) })?.minus(it)
            }
        val documentEntity =
            accountUtilizationRepository.getTDSDocumentList(
                request.pageLimit,
                offset,
                request.accType,
                request.orgId,
                request.accMode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )

        val documentModel = groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        val total =
            accountUtilizationRepository.getTDSDocumentCount(
                request.accType,
                request.orgId,
                request.accMode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        for (doc in documentModel) {
            // Fetch Organization Tds Profile
            val tdsProfile = getOrgTdsProfile(doc.organizationId)
            // Fetch Rate From Profile
            val rate = getTdsRate(tdsProfile)

            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType))
            doc.status = settlementServiceHelper.getDocumentStatus(
                afterTdsAmount = doc.afterTdsAmount,
                balanceAmount = doc.balanceAmount,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.tds = calculateTds(
                rate = rate,
                settledTds = doc.settledTds!!,
                taxableAmount = doc.taxableAmount
            )
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
        }
        documentModel.forEach { it.documentNo = hashId.encode(it.documentNo.toLong()) }
        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit!!) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    private fun validateTdsDocumentInput(request: TdsSettlementDocumentRequest) {
        if (request.orgId.isEmpty()) throw AresException(AresError.ERR_1003, "orgId")
        if (request.accMode == null) throw AresException(AresError.ERR_1003, "account mode")
    }

    override suspend fun check(request: CheckRequest): List<CheckDocument> =
        runSettlement(request, false)

    override suspend fun editCheck(request: CheckRequest): List<CheckDocument> {
        adjustBalanceAmount(type = "add", documents = request.stackDetails)
        val checkResponse = check(request)
        adjustBalanceAmount(type = "subtract", documents = request.stackDetails)
        return checkResponse
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settle(request: CheckRequest): List<CheckDocument> =
        runSettlement(request, true)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun edit(request: CheckRequest): List<CheckDocument> = editSettlement(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun editTds(request: EditTdsRequest) = editInvoiceTds(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun delete(documentNo: String, settlementType: SettlementType) =
        deleteSettlement(documentNo, settlementType)

    override suspend fun getOrgSummary(
        orgId: UUID,
        startDate: Timestamp?,
        endDate: Timestamp?
    ): OrgSummaryResponse {
        val responseEntity =
            accountUtilizationRepository.getOrgSummary(orgId, startDate, endDate)
                ?: throw AresException(AresError.ERR_1005, "")
        val responseModel = orgSummaryConverter.convertToModel(responseEntity)
        responseModel.tdsStyle = TdsStyle(style = "gross", rate = 2.toBigDecimal())
        return responseModel
    }

    private suspend fun editInvoiceTds(request: EditTdsRequest): String {
        request.documentNo = hashId.decode(request.documentNo!!)[0].toString()
        val doc =
            settlementRepository.findByDestIdAndDestType(
                request.documentNo!!.toLong(),
                request.settlementType!!
            )
        val tdsDoc =
            doc.find { it?.sourceType in listOf(SettlementType.CTDS, SettlementType.VTDS) }
                ?: throw AresException(AresError.ERR_1503, "TDS")
        val sourceDoc =
            doc.first { it.sourceType in fetchSettlingDocs(it?.destinationType!!) }
                ?: throw AresException(AresError.ERR_1503, "PAYMENT")
        val sourceLedgerRate =
            Utilities.binaryOperation(sourceDoc.ledAmount, sourceDoc.amount!!, Operator.DIVIDE)
        var currNewTds = request.newTds!!
        if (sourceDoc.currency != request.currency) {
            val rate =
                if (sourceDoc.ledCurrency == request.currency) {
                    sourceLedgerRate
                } else {
                    val accUt = accountUtilizationRepository.findRecord(
                        sourceDoc.sourceId!!,
                        sourceDoc.sourceType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            "${sourceDoc.sourceType}_${sourceDoc.sourceId}"
                        )
                    settlementServiceHelper.getExchangeRate(
                        sourceDoc.currency!!,
                        request.currency!!,
                        SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(accUt.transactionDate)
                    )
                }
            currNewTds = getExchangeValue(request.newTds!!, rate, true)
        }
        if (currNewTds > tdsDoc.amount!!) {
            // TODO("Generate Credit Note")
            return hashId.encode(tdsDoc.destinationId)
        } else if (currNewTds < tdsDoc.amount) {
            val invoiceTdsDiff = request.oldTds!! - request.newTds!!
            val paymentTdsDiff = tdsDoc.amount!! - currNewTds
            reduceAccountUtilization(
                tdsDoc.destinationId,
                AccountType.valueOf(tdsDoc.destinationType.toString()),
                invoiceTdsDiff,
                Utilities.binaryOperation(
                    invoiceTdsDiff,
                    request.exchangeRate!!,
                    Operator.MULTIPLY
                )
            )
            sourceDoc.amount = sourceDoc.amount?.minus(paymentTdsDiff)
            sourceDoc.ledAmount =
                sourceDoc.ledAmount.minus(
                    Utilities.binaryOperation(
                        paymentTdsDiff,
                        sourceLedgerRate,
                        Operator.MULTIPLY
                    )
                )
            settlementRepository.update(sourceDoc)
        }
        tdsDoc.amount = currNewTds
        tdsDoc.ledAmount =
            Utilities.binaryOperation(currNewTds, sourceLedgerRate, Operator.MULTIPLY)
        settlementRepository.update(tdsDoc)
        return hashId.encode(tdsDoc.destinationId)
    }

    private suspend fun editSettlement(request: CheckRequest): List<CheckDocument> {
        val sourceDoc =
            request.stackDetails.first {
                it.accountType in listOf(SettlementType.REC, SettlementType.PCN)
            }
        deleteSettlement(sourceDoc.documentNo, sourceDoc.accountType)
        return runSettlement(request, true)
    }

    private suspend fun deleteSettlement(documentNo: String, settlementType: SettlementType): String {
        val documentNo = hashId.decode(documentNo)[0]
        val sourceType =
            if (settlementType == SettlementType.REC)
                listOf(SettlementType.REC, SettlementType.CTDS, SettlementType.SECH)
            else listOf(SettlementType.PCN, SettlementType.VTDS, SettlementType.PECH)
        val fetchedDoc = settlementRepository.findBySourceIdAndSourceType(documentNo, sourceType)
        val debitDoc = fetchedDoc.groupBy { it?.destinationId }
        val sourceCurr =
            fetchedDoc.sumOf {
                it?.amount?.multiply(BigDecimal.valueOf(it.signFlag.toLong()))
                    ?: BigDecimal.ZERO
            }
        reduceAccountUtilization(
            documentNo,
            AccountType.valueOf(settlementType.toString()),
            sourceCurr
        )
        for (debits in debitDoc) {
            val settledDoc =
                debits.value.filter { it?.sourceType == settlementType }
            if (settledDoc.isEmpty()) throw AresException(AresError.ERR_1501, "")
            for (source in settledDoc) {
                val payment =
                    accountUtilizationRepository.findRecord(
                        source!!.sourceId!!,
                        source.sourceType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            source.sourceId.toString()
                        )
                val invoice =
                    accountUtilizationRepository.findRecord(
                        source.destinationId,
                        source.destinationType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            source.destinationId.toString()
                        )
                var settledCurr = source.amount!!
                if (payment.currency != invoice.currency) {
                    val rate =
                        if (payment.ledCurrency == invoice.currency) {
                            Utilities.binaryOperation(
                                payment.amountLoc,
                                payment.amountCurr,
                                Operator.DIVIDE
                            )
                        } else {
                            settlementServiceHelper.getExchangeRate(
                                payment.currency,
                                invoice.currency,
                                SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
                            )
                        }
                    settledCurr = getExchangeValue(settledCurr, rate)
                }
                reduceAccountUtilization(
                    source.destinationId,
                    AccountType.valueOf(source.destinationType.toString()),
                    settledCurr
                )
            }
        }
        settlementRepository.deleteByIdIn(fetchedDoc.map { it?.id!! })
        return hashId.encode(documentNo)
    }

    private suspend fun reduceAccountUtilization(
        docId: Long,
        accType: AccountType,
        amount: BigDecimal,
        ledAmount: BigDecimal? = null
    ) {
        val accUtil =
            accountUtilizationRepository.findRecord(docId, accType.toString())
                ?: throw AresException(AresError.ERR_1503, "${accType}_$docId")
        accUtil.payCurr -= amount
        accUtil.payLoc -=
            ledAmount
                ?: getExchangeValue(
                    amount,
                    Utilities.binaryOperation(
                        accUtil.amountLoc,
                        accUtil.amountCurr,
                        Operator.DIVIDE
                    )
                )
        val accUtilObj = accountUtilizationRepository.update(accUtil)
        try {
            updateExternalSystemInvoice(accUtilObj)
            OpenSearchClient().updateDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilObj.id.toString(), accUtilObj)
            emitDashboardAndOutstandingEvent(accUtilObj)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    private suspend fun runSettlement(
        request: CheckRequest,
        performDbOperation: Boolean
    ): List<CheckDocument> {
        val settledTdsCopy = storeSettledTds(request)
        sanitizeInput(request)
        val source = mutableListOf<CheckDocument>()
        val dest = mutableListOf<CheckDocument>()
        val creditType =
            listOf(
                SettlementType.REC,
                SettlementType.PCN,
                SettlementType.PAY,
                SettlementType.SCN
            )
        val debitType =
            listOf(
                SettlementType.SINV,
                SettlementType.PINV,
                SettlementType.SDN,
                SettlementType.PDN
            )
        for (doc in request.stackDetails.reversed()) {
            if (creditType.contains(doc.accountType)) {
                source.add(doc)
            } else if (debitType.contains(doc.accountType)) {
                dest.add(doc)
            }
        }
        if (source.isEmpty() &&
            dest.map { it.accountType }.contains(SettlementType.SINV) &&
            dest.map { it.accountType }.contains(SettlementType.PINV)
        ) {
            dest.filter { it.accountType == SettlementType.SINV }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }
        businessValidation(source, dest)
        val settledList = settleDocuments(request, source, dest, performDbOperation)
        settledList.forEach {
            it.documentNo = hashId.encode(it.documentNo.toLong())
            it.settledTds = settledTdsCopy[it.id]!!
        }
        return request.stackDetails.map { r -> settledList.filter { it.id == r.id }[0] }
    }

    /**
     * Settle documents: source to destination.
     */
    private suspend fun settleDocuments(
        request: CheckRequest,
        source: MutableList<CheckDocument>,
        dest: MutableList<CheckDocument>,
        performDbOperation: Boolean
    ): MutableList<CheckDocument> {
        val response = mutableListOf<CheckDocument>()
        for (payment in source) {
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType) &&
                    availableAmount.compareTo(0.toBigDecimal()) != 0
                ) {
                    availableAmount =
                        doSettlement(
                            request,
                            invoice,
                            availableAmount,
                            payment,
                            source,
                            performDbOperation
                        )
                }
                if (payment.tds!!.compareTo(BigDecimal.ZERO) != 0 &&
                    payment.settledTds.compareTo(BigDecimal.ZERO) == 0 &&
                    performDbOperation
                ) {
                    createTdsRecord(
                        sourceId = invoice.documentNo.toLong(),
                        destId = payment.documentNo.toLong(),
                        destType = payment.accountType,
                        currency = payment.currency,
                        ledCurrency = payment.ledCurrency,
                        tdsAmount = payment.tds!!,
                        tdsLedAmount = getExchangeValue(payment.tds!!, payment.exchangeRate),
                        settlementDate = request.settlementDate,
                        signFlag = 1,
                        createdBy = request.createdBy
                    )
                    payment.settledTds += payment.tds!!
                }
            }
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation =
                payment.balanceAmount.subtract(payment.allocationAmount)
            assignPaymentStatus(payment)
            response.add(payment)
        }
        dest.forEach { response.add(it) }
        return response
    }

    private suspend fun doSettlement(
        request: CheckRequest,
        invoice: CheckDocument,
        availableAmount: BigDecimal,
        payment: CheckDocument,
        source: MutableList<CheckDocument>,
        performDbOperation: Boolean
    ): BigDecimal {
        var amount = availableAmount
        val toSettleAmount = invoice.allocationAmount - invoice.settledAllocation
        if (toSettleAmount.compareTo(BigDecimal.ZERO) != 0) {
            var rate = 1.toBigDecimal()
            val ledgerRate = payment.exchangeRate
            var updateDoc = true
            if (payment.currency != invoice.currency) {
                rate =
                    if (payment.ledCurrency == invoice.currency) {
                        ledgerRate
                    } else {
                        settlementServiceHelper.getExchangeRate(
                            payment.currency,
                            invoice.currency,
                            SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
                        )
                    }
                amount = getExchangeValue(availableAmount, rate)
            }
            if (amount >= toSettleAmount) {
                amount =
                    updateDocuments(
                        request,
                        invoice,
                        payment,
                        toSettleAmount,
                        amount,
                        rate,
                        ledgerRate,
                        true,
                        performDbOperation
                    )
            } else if (amount < toSettleAmount) {
                if (payment != source.last()) updateDoc = false
                amount =
                    updateDocuments(
                        request,
                        invoice,
                        payment,
                        amount,
                        amount,
                        rate,
                        ledgerRate,
                        updateDoc,
                        performDbOperation
                    )
            }
        }
        return amount
    }

    private suspend fun updateDocuments(
        request: CheckRequest,
        invoice: CheckDocument,
        payment: CheckDocument,
        toSettleAmount: BigDecimal,
        availableAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        updateDoc: Boolean,
        performDbOperation: Boolean
    ): BigDecimal {
        val amount = availableAmount - toSettleAmount
        invoice.settledAllocation += toSettleAmount
        payment.settledAllocation +=
            getExchangeValue(toSettleAmount, exchangeRate, true)
        if (updateDoc) {
            invoice.allocationAmount = invoice.settledAllocation
            invoice.balanceAfterAllocation =
                invoice.balanceAmount.subtract(invoice.allocationAmount)
        }
        assignInvoiceStatus(invoice)
        assignPaymentStatus(payment)
        if (performDbOperation)
            performDbOperation(
                request,
                toSettleAmount,
                exchangeRate,
                ledgerRate,
                payment,
                invoice
            )
        return getExchangeValue(amount, exchangeRate, true)
    }

    private suspend fun performDbOperation(
        request: CheckRequest,
        toSettleAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        payment: CheckDocument,
        invoice: CheckDocument
    ) {
        val paidAmount = getExchangeValue(toSettleAmount, exchangeRate, true)
        val paidLedAmount = getExchangeValue(paidAmount, ledgerRate)
        val invoiceTds = invoice.tds!! - invoice.settledTds
        val paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        val paymentTdsLed = getExchangeValue(paymentTds, ledgerRate)
        createSettlement(
            payment.documentNo.toLong(),
            payment.accountType,
            invoice.documentNo.toLong(),
            invoice.accountType,
            payment.currency,
            (paidAmount + paymentTds),
            payment.ledCurrency,
            (paidLedAmount + paymentTdsLed),
            1,
            request.settlementDate,
            request.createdBy,
        )
        if (paymentTds.compareTo(0.toBigDecimal()) != 0) {
            createTdsRecord(
                sourceId = payment.documentNo.toLong(),
                destId = invoice.documentNo.toLong(),
                destType = invoice.accountType,
                currency = payment.currency,
                ledCurrency = payment.ledCurrency,
                tdsAmount = paymentTds,
                tdsLedAmount = paymentTdsLed,
                settlementDate = request.settlementDate,
                signFlag = -1,
                createdBy = request.createdBy
            )
            invoice.settledTds += invoiceTds
        }
        if (payment.ledCurrency != invoice.currency) {
            val excLedAmount =
                getExchangeValue(toSettleAmount, invoice.exchangeRate) - (paidLedAmount)
            val exType =
                if (fetchSettlingDocs(SettlementType.CTDS).contains(invoice.accountType))
                    SettlementType.SECH
                else SettlementType.PECH
            val exSign =
                excLedAmount.signum() *
                    if (payment.accountType in listOf(SettlementType.SCN, SettlementType.REC, SettlementType.SINV)) {
                        -1
                    } else {
                        1
                    }
            createSettlement(
                payment.documentNo.toLong(),
                exType,
                invoice.documentNo.toLong(),
                invoice.accountType,
                null,
                null,
                invoice.ledCurrency,
                excLedAmount.abs(),
                exSign.toShort(),
                request.settlementDate,
                request.createdBy
            )
        }
        val paymentUtilized =
            paidAmount +
                if (payment.accountType in listOf(SettlementType.PCN, SettlementType.SCN))
                    payment.tds!!
                else 0.toBigDecimal()
        updateAccountUtilization(payment, paymentUtilized)
        updateAccountUtilization(invoice, (toSettleAmount + invoiceTds))
    }

    private suspend fun createTdsRecord(
        sourceId: Long?,
        destId: Long,
        destType: SettlementType,
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        signFlag: Short,
        settlementDate: Timestamp,
        createdBy: UUID?
    ) {
        val tdsType =
            if (fetchSettlingDocs(SettlementType.CTDS).contains(destType)) {
                SettlementType.CTDS
            } else {
                SettlementType.VTDS
            }
        createSettlement(
            sourceId,
            tdsType,
            destId,
            destType,
            currency,
            tdsAmount,
            ledCurrency,
            tdsLedAmount,
            signFlag,
            settlementDate,
            createdBy
        )
    }

    private suspend fun updateAccountUtilization(
        document: CheckDocument,
        utilizedAmount: BigDecimal
    ) {
        val paymentUtilization =
            accountUtilizationRepository.findRecord(
                document.documentNo.toLong(),
                document.accountType.toString()
            )
                ?: throw AresException(
                    AresError.ERR_1503,
                    "${document.documentNo}_${document.accountType}"
                )
        if ((paymentUtilization.amountCurr - paymentUtilization.payCurr) < utilizedAmount) {
            throw AresException(AresError.ERR_1504, " Document No: ${paymentUtilization.documentValue}")
        }
        paymentUtilization.payCurr += utilizedAmount
        paymentUtilization.payLoc += getExchangeValue(utilizedAmount, document.exchangeRate)
        val accountUtilization = accountUtilizationRepository.update(paymentUtilization)
        try {
            updateExternalSystemInvoice(accountUtilization)
            OpenSearchClient().updateDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, paymentUtilization.id.toString(), paymentUtilization)
            emitDashboardAndOutstandingEvent(paymentUtilization)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    /**
     * Invokes Kafka for Plutus(Sales) or Kuber(Purchase) based on accountType in accountUtilization.
     * @param: accountUtilization
     */
    private fun updateExternalSystemInvoice(accountUtilization: AccountUtilization) {
        when (accountUtilization.accType) {
            AccountType.PINV -> emitPayableBillStatus(accountUtilization)
            AccountType.SINV -> updateBalanceAmount(accountUtilization)
            else -> {}
        }
    }

    /**
     * Invokes Kafka event to update balanceAmount in Plutus(Sales MS).
     * @param: accountUtilization
     */
    private fun updateBalanceAmount(accountUtilization: AccountUtilization) {
        aresKafkaEmitter.emitInvoiceBalance(
            invoiceBalanceEvent = UpdateInvoiceBalanceEvent(
                invoiceBalance = InvoiceBalance(
                    invoiceId = accountUtilization.documentNo,
                    balanceAmount = accountUtilization.amountCurr.minus(accountUtilization.payCurr)
                )
            )
        )
    }

    /**
     * Invokes Kafka event to update status in Kuber(Purchase MS).
     * @param: accountUtilization
     */
    private fun emitPayableBillStatus(accountUtilization: AccountUtilization) {
        val status = if (accountUtilization.payLoc.compareTo(BigDecimal.ZERO) == 0)
            "UNPAID"
        else if (accountUtilization.amountCurr > accountUtilization.payCurr)
            "PARTIAL"
        else
            "FULL"

        aresKafkaEmitter.emitBillPaymentStatus(
            PayableKnockOffProduceEvent(
                AccountPayableFileResponse(
                    documentNo = accountUtilization.documentNo,
                    documentValue = accountUtilization.documentValue!!,
                    isSuccess = true,
                    paymentStatus = status,
                    failureReason = null
                )
            )
        )
    }

    private fun emitDashboardAndOutstandingEvent(
        accUtilizationRequest: AccountUtilization
    ) {
        emitDashboardData(accUtilizationRequest)
        if (accUtilizationRequest.accMode == AccMode.AR) {
            emitOutstandingData(accUtilizationRequest)
        }
    }

    private fun emitDashboardData(accUtilizationRequest: AccountUtilization) {
        val date: Date = accUtilizationRequest.transactionDate!!
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(date),
                    quarter = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .get(IsoFields.QUARTER_OF_YEAR),
                    year = date.toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate().year,
                    accMode = accUtilizationRequest.accMode
                )
            )
        )
    }

    private fun emitOutstandingData(accUtilizationRequest: AccountUtilization) {
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

    private suspend fun createSettlement(
        sourceId: Long?,
        sourceType: SettlementType,
        destId: Long,
        destType: SettlementType,
        currency: String?,
        amount: BigDecimal?,
        ledCurrency: String,
        ledAmount: BigDecimal,
        signFlag: Short,
        transactionDate: Timestamp,
        createdBy: UUID?
    ) {
        val settledDoc =
            Settlement(
                null,
                sourceId,
                sourceType,
                destId,
                destType,
                currency,
                amount,
                ledCurrency,
                ledAmount,
                signFlag,
                transactionDate,
                createdBy,
                Timestamp.from(Instant.now()),
                createdBy,
                Timestamp.from(Instant.now())
            )
        settlementRepository.save(settledDoc)
    }

    private fun getExchangeValue(
        amount: BigDecimal,
        exchangeRate: BigDecimal,
        reverse: Boolean = false
    ): BigDecimal {
        return if (reverse) {
            Utilities.binaryOperation(amount, exchangeRate, Operator.DIVIDE)
        } else {
            Utilities.binaryOperation(amount, exchangeRate, Operator.MULTIPLY)
        }
    }

    private fun fetchSettlingDocs(accType: SettlementType): List<SettlementType> {
        return when (accType) {
            SettlementType.REC -> {
                listOf(SettlementType.SINV, SettlementType.SDN)
            }
            SettlementType.PINV -> {
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)
            }
            SettlementType.PCN -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            SettlementType.PAY -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            SettlementType.SINV -> {
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.PINV)
            }
            SettlementType.SCN -> {
                listOf(SettlementType.SINV, SettlementType.SDN)
            }
            SettlementType.SDN -> {
                listOf(SettlementType.SCN, SettlementType.REC)
            }
            SettlementType.PDN -> {
                listOf(SettlementType.PCN, SettlementType.PAY)
            }
            SettlementType.CTDS -> {
                listOf(SettlementType.SINV, SettlementType.SDN, SettlementType.SCN)
            }
            SettlementType.VTDS -> {
                listOf(SettlementType.PINV, SettlementType.PDN, SettlementType.PCN)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun storeSettledTds(request: CheckRequest): MutableMap<Long, BigDecimal> {
        val settledTdsCopy = mutableMapOf<Long, BigDecimal>()
        request.stackDetails.forEach {
            settledTdsCopy.put(it.id, it.settledTds)
        }
        return settledTdsCopy
    }

    private fun sanitizeInput(request: CheckRequest) {
        request.stackDetails.forEach {
            it.documentNo = hashId.decode(it.documentNo)[0].toString()
            it.settledAllocation = BigDecimal.ZERO
            it.settledTds = BigDecimal.ZERO
        }
    }

    /**
     * Run business Validations on source and destination document list.
     * @param: source
     * @param: dest
     */
    private fun businessValidation(
        source: MutableList<CheckDocument>,
        dest: MutableList<CheckDocument>
    ) {
        var creditCount = 0
        var debitCount = 0
        for (payment in source) {
            fetchSettlingDocs(payment.accountType).forEach { debit ->
                if (dest.map { it.accountType }.contains(debit)) debitCount += 1
            }
            if (debitCount == 0) throw AresException(AresError.ERR_1502, "") else debitCount = 0
        }
        for (invoice in dest) {
            fetchSettlingDocs(invoice.accountType).forEach { credit ->
                if (source.map { it.accountType }.contains(credit)) creditCount += 1
            }
            if (creditCount == 0) throw AresException(AresError.ERR_1501, "") else creditCount = 0
        }
    }

    private fun assignInvoiceStatus(doc: CheckDocument) {
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 0) {
            doc.status = DocStatus.KNOCKED_OFF.value
        } else if (decimalRound(doc.settledAllocation).compareTo(0.toBigDecimal()) == 0) {
            doc.status = DocStatus.UNPAID.value
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 1) {
            doc.status = DocStatus.PARTIAL_PAID.value
        }
    }

    private fun assignPaymentStatus(doc: CheckDocument) {
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 0) {
            doc.status = DocStatus.UTILIZED.value
        } else if (decimalRound(doc.settledAllocation).compareTo(0.toBigDecimal()) == 0) {
            doc.status = DocStatus.UNUTILIZED.value
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 1) {
            doc.status = DocStatus.PARTIAL_UTILIZED.value
        }
    }

    private fun decimalRound(amount: BigDecimal): BigDecimal {
        return Utilities.decimalRound(amount)
    }

    private fun adjustBalanceAmount(type: String, documents: List<CheckDocument>): List<CheckDocument> {
        if (type == "add") {
            documents.forEach { it.balanceAmount += it.settledAmount ?: BigDecimal.ZERO }
        }
        if (type == "subtract") {
            documents.forEach { it.balanceAmount -= it.settledAmount ?: BigDecimal.ZERO }
        }
        return documents
    }
}
