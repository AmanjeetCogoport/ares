package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.SageStatus.Companion.getZstatus
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.IncidentStatus
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.ListOrgStylesRequest
import com.cogoport.ares.api.common.models.TdsStylesResponse
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.events.PlutusMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.sage.service.interfaces.SageService
import com.cogoport.ares.api.settlement.entity.IncidentMappings
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.entity.SettlementListDoc
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.OrgSummaryMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.model.PaymentInfo
import com.cogoport.ares.api.settlement.model.Sid
import com.cogoport.ares.api.settlement.repository.IncidentMappingsRepository
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.DocType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.Operator
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckResponse
import com.cogoport.ares.model.settlement.CreateIncidentRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.TdsStyle
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.ares.model.settlement.event.InvoiceBalance
import com.cogoport.ares.model.settlement.event.PaymentInfoRec
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.OrgSummaryRequest
import com.cogoport.ares.model.settlement.request.PostSettlementRequest
import com.cogoport.ares.model.settlement.request.RejectSettleApproval
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.sage.SageException
import com.cogoport.brahma.sage.model.request.SageSettlementRequest
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.kuber.client.KuberClient
import com.cogoport.kuber.model.bills.BillDocResponse
import com.cogoport.kuber.model.bills.ListBillRequest
import com.cogoport.kuber.model.bills.request.UpdatePaymentStatusRequest
import com.cogoport.plutus.client.PlutusClient
import com.cogoport.plutus.model.common.enums.TransactionType
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.TransactionDocuments
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import io.micronaut.http.client.exceptions.ReadTimeoutException
import io.sentry.Sentry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.XML
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil
import com.cogoport.brahma.sage.Client as SageClient

@Singleton
open class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accutilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var settlementRepository: SettlementRepository

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
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var incidentMappingsRepository: IncidentMappingsRepository

    @Inject
    lateinit var kuberClient: KuberClient

    @Value("\${ares.settlement.crossTradeParty:false}")
    private var crossTradeParty: Boolean = false

    @Inject
    private lateinit var journalVoucherService: JournalVoucherService

    @Inject
    lateinit var onAccountService: OnAccountService

    @Inject
    private lateinit var paymentRepo: PaymentRepository

    @Inject
    private lateinit var invoicePaymentMappingRepo: InvoicePayMappingRepository

    @Inject lateinit var railsClient: RailsClient

    @Inject lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var plutusMessagePublisher: PlutusMessagePublisher

    @Inject
    lateinit var kuberMessagePublisher: KuberMessagePublisher

    @Inject
    lateinit var sageService: SageService

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var accUtilizationToPaymentConverter: AccUtilizationToPaymentMapper

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var util: Util

    @Inject
    lateinit var parentJvService: ParentJVService

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    private lateinit var accountUtilizationRepo: AccountUtilizationRepo

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
        val orgId = listOf(summaryRequest.orgId)
        val validAccTypeForOpenInvoiceCalculation = listOf(AccountType.PINV, AccountType.PCN, AccountType.PREIMB, AccountType.SINV, AccountType.SCN, AccountType.SREIMBCN, AccountType.SREIMB, AccountType.EXP)
        val validAccTypeForOnAccountCalculation = listOf(AccountType.VTDS, AccountType.REC, AccountType.CTDS, AccountType.PAY, AccountType.BANK, AccountType.CONTR, AccountType.ROFF, AccountType.MTCCV, AccountType.MISC, AccountType.INTER, AccountType.OPDIV, AccountType.MTC)
        val openInvoiceAmount =
            accountUtilizationRepository.getAccountBalance(
                orgId,
                summaryRequest.entityCode!!,
                summaryRequest.startDate,
                summaryRequest.endDate,
                validAccTypeForOpenInvoiceCalculation,
                summaryRequest.accModes?.map { it.name }
            )
        val onAccountPayment =
            accountUtilizationRepository.getAccountBalance(
                orgId,
                summaryRequest.entityCode!!,
                null,
                null,
                validAccTypeForOnAccountCalculation,
                summaryRequest.accModes?.map { it.name }
            )
        return SummaryResponse(
            openInvoiceAmount = openInvoiceAmount,
            onAccountAmount = onAccountPayment,
            outstandingAmount = openInvoiceAmount + onAccountPayment,
            ledgerCurrency = AresConstants.LEDGER_CURRENCY[summaryRequest.entityCode]
        )
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

        var paymentIds: List<Long> = emptyList()
        if (!request.query.isNullOrEmpty()) {
            paymentIds = settlementRepository.getPaymentIds(query = request.query!!)
        } else {
            request.query = ""
        }

        val documents =
            accountUtilizationRepository.getHistoryDocument(
                request.orgId!!,
                accountTypes,
                request.page,
                request.pageLimit,
                request.startDate,
                request.endDate,
                request.query,
                paymentIds,
                request.sortBy!!,
                request.sortType!!,
                request.entityCode
            )

        val totalRecords =
            accountUtilizationRepository.countHistoryDocument(
                request.orgId!!,
                accountTypes,
                request.startDate,
                request.endDate,
                request.query,
                paymentIds,
                request.entityCode
            )

        val historyDocuments = mutableListOf<HistoryDocument>()
        documents.forEach { doc ->
            historyDocuments.add(historyDocumentConverter.convertToModel(doc))
        }
        historyDocuments.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = it.id?.let { it1 -> Hashids.encode(it1.toLong()) }
        }
        return ResponseList(
            list = historyDocuments,
            totalPages = Utilities.getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private fun stringAccountTypes(request: SettlementHistoryRequest): MutableList<String> {
        val possibleAccountTypes = settlementServiceHelper.getJvList(AccountType::class.java).map { it -> it.name }.toMutableList() + mutableListOf(
            AccountType.PCN.toString(),
            AccountType.SINV.toString(), AccountType.SCN.toString(),
            AccountType.REC.toString(),
            AccountType.PAY.toString()
        )
        val accountTypes =
            if (request.accountType == AresConstants.ALL) {
                possibleAccountTypes.toMutableList()
            } else if (request.accountType == "REC") {
                mutableListOf(AccountType.REC.toString(), AccountType.PAY.toString())
            } else if (request.accountType == "SINV") {
                mutableListOf(AccountType.SINV.toString())
            } else {
                mutableListOf(request.accountType!!)
            }
        return accountTypes
    }

    /**
     * Get Settlement details for input document number
     * @param request
     * @return ResponseList
     */
    override suspend fun getSettlement(
        request: SettlementRequest
    ): ResponseList<com.cogoport.ares.model.settlement.SettledInvoice?> {
        request.documentNo = Hashids.decode(request.documentNo)[0].toString()
        val settlementGrouped = getSettlementFromDB(request)
        val paymentIds = mutableListOf(request.documentNo.toLong())
        val payments = getPaymentDataForSettledInvoices(settlementGrouped, paymentIds, request.settlementType)
        val settlements = getSettledInvoices(settlementGrouped, payments, request.documentNo.toLong())
        // Fetch Sid for invoices
        val docIds = settlements.map { it.destinationId.toString() }
        val sids = getSidsForInvoices(docIds, request.settlementType)
        val settledDocuments = populateSettledDocuments(settlements, request, payments, sids)

        // Pagination Data
        val totalRecords =
            settlementRepository.countSettlement(request.documentNo.toLong(), request.settlementType)
        settledDocuments.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = it.id?.let { it1 -> Hashids.encode(it1.toLong()) }
        }
        return ResponseList(
            list = settledDocuments,
            totalPages = Utilities.getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private suspend fun getSidsForInvoices(ids: List<String>, accType: SettlementType): List<Sid>? {
        return if (ids.isNotEmpty()) {
            try {
                if (accType == SettlementType.REC) {
                    plutusClient.getSidsForInvoiceIds(ids)?.map {
                        Sid(
                            documentId = it.invoiceId,
                            jobNumber = it.jobNumber
                        )
                    }
                } else if (accType in listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)) {
                    kuberClient.getSidsForBillIds(ids).map {
                        Sid(
                            documentId = it.billId,
                            jobNumber = it.jobNumber
                        )
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
                null
            }
        } else null
    }

    private suspend fun getSettledInvoices(
        settlementGrouped: Map<Long?, List<SettledInvoice>>,
        payments: List<PaymentData>,
        paymentRequested: Long
    ): MutableList<SettledInvoice> {
        val settlements = mutableListOf<SettledInvoice>()
        for (docList in settlementGrouped) {
            val settledTds = docList.value.sumOf { doc ->
                if (!doc.tdsCurrency.isNullOrBlank()) {
                    val rate = payments.find { it.documentNo == doc.tdsDocumentNo }?.exchangeRate
                    val date = payments.find { it.documentNo == doc.tdsDocumentNo }?.transactionDate
                        ?: throw AresException(AresError.ERR_1503, "${doc.tdsDocumentNo}")
                    convertPaymentCurrToInvoiceCurr(
                        toCurrency = doc.currency!!,
                        fromCurrency = doc.tdsCurrency,
                        fromLedCurrency = doc.ledCurrency,
                        amount = doc.settledTds,
                        exchangeRate = rate,
                        exchangeDate = date
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
            docList.value.map {
                it.settledTds = settledTds
            }
            val settledInvoice = docList.value.find { it.tdsDocumentNo == paymentRequested } ?: docList.value.first()
            settlements.add(settledInvoice)
        }
        return settlements
    }

    private suspend fun populateSettledDocuments(
        settlements: MutableList<SettledInvoice>,
        request: SettlementRequest,
        payments: List<PaymentData>,
        sids: List<Sid>?
    ): MutableList<com.cogoport.ares.model.settlement.SettledInvoice> {
        val settledDocuments = mutableListOf<com.cogoport.ares.model.settlement.SettledInvoice>()
        val possibleAccType = settlementServiceHelper.getJvList(SettlementType::class.java).toMutableList() + mutableListOf(
            SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SINV, SettlementType.SCN
        )
        settlements.forEach { settlement ->
            if (possibleAccType.contains(request.settlementType)) {
                // Calculate Settled Amount in Invoice Currency
                settlement.settledAmount =
                    getAmountInInvoiceCurrency(settlement, payments, settlement.settledAmount)
                // Calculate Tds in Invoice Currency
                settlement.tds = getAmountInInvoiceCurrency(settlement, payments, settlement.tds)
                // Calculate Nostro in Invoice Currency
                settlement.nostroAmount =
                    getAmountInInvoiceCurrency(settlement, payments, settlement.nostroAmount)

                // Convert To Model
                val settledDoc = settledInvoiceConverter.convertToModel(settlement)
                settledDoc.settledAmount -= (settledDoc.tds + settledDoc.nostroAmount)
                settledDoc.balanceAmount = settledDoc.currentBalance
                settledDoc.allocationAmount = settledDoc.settledAmount
                settledDoc.afterTdsAmount -= settledDoc.settledTds
                settledDoc.currentBalance += (settledDoc.tds + settledDoc.nostroAmount)
                settledDoc.settledTds -= settledDoc.tds

                // Assign Sid
                settledDoc.sid = sids?.find { it.documentId == settledDoc.documentNo.toLong() }?.jobNumber
                // Assign Status
                val paid = (settledDoc.documentAmount - (settledDoc.settledAmount + settledDoc.tds + settledDoc.nostroAmount))
                if (paid.compareTo(BigDecimal.ZERO) == 0) {
                    settledDoc.status = DocStatus.KNOCKED_OFF.value
                } else if (paid.compareTo(settledDoc.documentAmount) == 0) {
                    settledDoc.status = DocStatus.UNPAID.value
                } else {
                    settledDoc.status = DocStatus.PARTIAL_PAID.value
                }
                settledDocuments.add(settledDoc)
            }
        }
        return settledDocuments
    }

    private suspend fun getAmountInInvoiceCurrency(
        settlement: SettledInvoice,
        payments: List<PaymentData>,
        amountPaymentCurr: BigDecimal
    ) = convertPaymentCurrToInvoiceCurr(
        toCurrency = settlement.currency!!,
        fromCurrency = settlement.paymentCurrency,
        fromLedCurrency = settlement.ledCurrency,
        amount = amountPaymentCurr,
        exchangeRate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.exchangeRate,
        exchangeDate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.transactionDate!!
    )

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
        val tdsType = mutableListOf(settlementType)
        settlementGrouped.forEach { docList ->
            docList.value.forEach {
                if (it.tdsDocumentNo != null)
                    paymentIds.add(it.tdsDocumentNo)
                it.tdsType?.let { it1 ->
                    when (it1) {
                        SettlementType.CTDS -> tdsType.addAll(
                            listOf(SettlementType.REC, SettlementType.SCN, SettlementType.SINV, SettlementType.CTDS)
                        )
                        SettlementType.VTDS -> tdsType.addAll(
                            listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV, SettlementType.VTDS, SettlementType.EXP)
                        )
                        else -> tdsType.add(it1)
                    }
                }
            }
        }

        val payments = accountUtilizationRepository.getPaymentDetails(paymentIds.distinct(), tdsType.distinct())
        payments.forEach {
            if (it.documentNo == null) throw AresException(AresError.ERR_1503, "")
            if (it.transactionDate == null) throw AresException(AresError.ERR_1005, "transactionDate")
        }
        return payments
    }

    /**
     * Get settled document from DB for the payment in request.
     * @param: request
     * @return: Map<Long?, List<SettledInvoice>>
     */
    private suspend fun getSettlementFromDB(request: SettlementRequest): Map<Long?, List<SettledInvoice>> {
        @Suppress("UNCHECKED_CAST")
        val settlements =
            settlementRepository.findSettlement(
                request.documentNo.toLong(),
                request.settlementType,
                request.page,
                request.pageLimit
            ) as MutableList<SettledInvoice>

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
        val orgId: List<UUID?>? = if (request.orgId != null) { listOf(request.orgId) } else { null }
        val accTypes = getAccountModeAndType(request.accModes, request.docType)

        val query = util.toQueryString(request.query)
        val documentEntity =
            accutilizationRepo.getDocumentList(
                request.pageLimit,
                offset,
                accTypes,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                query,
                request.accModes?.map { it.name },
                request.sortBy,
                request.sortType,
                request.documentPaymentStatus,
                request.docValues
            )
        if (documentEntity.isEmpty()) return ResponseList()

        val documentModel = calculatingTds(documentEntity, request.entityCode)

        val total = if (request.countRequired == true) {
            accutilizationRepo.getDocumentCount(
                accTypes,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                query,
                request.documentPaymentStatus,
                request.docValues
            )
        } else {
            0L
        }

        val billListIds = documentModel.filter { it.accountType in listOf("PINV", "PREIMB", "EXP") }.map { it.documentNo }

        val listBillRequest: ListBillRequest
        var responseList: com.cogoport.kuber.model.common.ResponseList<BillDocResponse>? = null
        if (billListIds.isNotEmpty()) {
            listBillRequest = ListBillRequest(
                jobNumbers = null,
                jobType = null,
                status = listOf("FINANCE_ACCEPTED"),
                excludeStatus = null,
                organizationId = null,
                serviceProviderOrgId = null,
                paymentStatus = null,
                serviceType = null,
                billNumber = null,
                urgencyTag = null,
                from = null,
                to = null,
                q = null,
                billType = null,
                proforma = null,
                serviceOpIds = null,
                billIds = billListIds
            )
            responseList = kuberClient.billListByIds(listBillRequest)
        }

        responseList?.list?.map { it ->
            val documentId = it.billId
            if (documentModel.any { k -> k.documentNo == documentId }) {
                documentModel.first { k -> k.documentNo == documentId }.hasPayrun = it.hasPayrun!!
            }
        }

        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    /**
     * Get TDS Deduction styles for list of trade party mapping id.
     * @param: tradePartyMappingIds
     * @return: List
     */
    private suspend fun listOrgTdsProfile(tradePartyMappingIds: List<String>): List<TdsStylesResponse> {
        var tdsStylesResponse = mutableListOf<TdsStylesResponse>()
        var tdsStylesFromClient: List<TdsStylesResponse>? = null
        try {
            tdsStylesFromClient = cogoClient.listOrgTdsStyles(request = ListOrgStylesRequest(ids = tradePartyMappingIds))?.data
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
        tdsStylesResponse = assignClientResponse(tdsStylesResponse, tradePartyMappingIds, tdsStylesFromClient)

        return tdsStylesResponse
    }

    /**
     * Assign client response TDS Styles to all the mapping Ids.
     * @param: tdsStylesResponse
     * @param: tradePartyMappingIds
     */
    private fun assignClientResponse(
        tdsStylesResponse: MutableList<TdsStylesResponse>,
        tradePartyMappingIds: List<String>,
        tdsStylesFromClient: List<TdsStylesResponse>?
    ): MutableList<TdsStylesResponse> {
        for (tradePartyMapping in tradePartyMappingIds) {
            val tdsElement = tdsStylesFromClient?.find { it.id.toString() == tradePartyMapping }
            if (tdsElement != null) {
                tdsStylesResponse.add(tdsElement)
            } else {
                addDefaultStyle(tdsStylesResponse, tradePartyMapping)
            }
        }
        return tdsStylesResponse
    }

    /**
     * Assign and add Default profile to input mapping Id.
     * @param: tdsStylesResponse
     * @param: tradePartyMappingIds
     */
    private fun addDefaultStyle(
        tdsStylesResponse: MutableList<TdsStylesResponse>,
        tradePartyMapping: String
    ) {
        tdsStylesResponse.add(
            TdsStylesResponse(
                id = UUID.fromString(tradePartyMapping),
                tdsDeductionStyle = "gross",
                tdsDeductionType = "normal",
                tdsDeductionRate = AresConstants.DEFAULT_TDS_RATE.toBigDecimal()
            )
        )
    }

    /**
     * Get TDS Deduction styles for trade party detail's self mapping id.
     * @param: tradePartyDetailsId
     * @return: TdsStylesResponse
     */
    private suspend fun getSelfOrgTdsProfile(tradePartyDetailsId: UUID): TdsStylesResponse {
        var tdsStylesResponse = TdsStylesResponse(
            id = tradePartyDetailsId,
            tdsDeductionStyle = "gross",
            tdsDeductionType = "no_deductions",
            tdsDeductionRate = 2.toBigDecimal()
        )
        try {
            tdsStylesResponse = cogoClient.getSelfOrgTdsStyles(tradePartyDetailsId.toString()).data
        } catch (_: Exception) {
            null
        }
        return tdsStylesResponse
    }

    /**
     * Groups document in case of.
     * @param: documentEntity
     * @return: List
     */
    suspend fun groupDocumentList(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>): List<com.cogoport.ares.api.settlement.entity.Document?> {
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

    /**
     * Get organization id list from importerExporterId and serviceProviderId.
     * @param: importerExporterId
     * @param: serviceProviderId
     * @return: List
     */
    private fun getOrgIds(importerExporterId: UUID?, serviceProviderId: UUID?): List<UUID> {
        val orgId = mutableListOf<UUID>()
        if (importerExporterId != null)
            orgId.add(importerExporterId)
        if (serviceProviderId != null)
            orgId.add(serviceProviderId)
        return orgId
    }

    /**
     * Get Account Mode and Account Type based on importerExportId and serviceProviderId in the request.
     * @param: importerExporterId
     * @param: serviceProviderId
     * @return: AccTypeMode
     */
    private fun getAccountModeAndType(accModeList: List<AccMode>?, docType: String?): List<AccountType>? {
        return accModeList
            ?.flatMap { getListOfAccountTypeFromDocType(docType, it) }
            ?.distinct()
    }

    private fun getListOfAccountTypeFromDocType(docType: String?, accMode: AccMode?): List<AccountType> {
        val jvList = settlementServiceHelper.getJvList(classType = AccountType::class.java).filter { it != AccountType.NEWPR }
        return when {
            docType == AresConstants.PAYMENT && accMode == AccMode.AR -> { listOf(AccountType.REC) }
            docType == AresConstants.PAYMENT && accMode == AccMode.AP -> { listOf(AccountType.PAY) }
            docType == AresConstants.INVOICE && accMode == AccMode.AR -> { listOf(AccountType.SINV, AccountType.SREIMB) }
            docType == AresConstants.INVOICE && accMode == AccMode.AP -> { listOf(AccountType.PINV, AccountType.PREIMB, AccountType.EXP) }
            docType == AresConstants.CREDIT_NOTE && accMode == AccMode.AR -> { listOf(AccountType.SCN, AccountType.SREIMBCN) }
            docType == AresConstants.CREDIT_NOTE && accMode == AccMode.AP -> { listOf(AccountType.PCN) }
            docType == AresConstants.TDS && accMode == AccMode.AR -> { listOf(AccountType.CTDS) }
            docType == AresConstants.TDS && accMode == AccMode.AP -> { listOf(AccountType.VTDS) }
            docType == AresConstants.TDS && accMode == AccMode.VTDS -> { listOf(AccountType.VTDS) }
            docType == AresConstants.JV -> { jvList }
            docType == AresConstants.INVOICE && accMode == null -> { listOf(AccountType.SINV, AccountType.PINV, AccountType.EXP) }
            docType == AresConstants.TDS && accMode == null -> { listOf(AccountType.VTDS, AccountType.CTDS) }
            docType == AresConstants.CREDIT_NOTE && accMode == null -> { listOf(AccountType.PCN, AccountType.SCN) }
            docType == null && accMode == AccMode.AR -> {
                listOf(AccountType.SINV, AccountType.REC, AccountType.SCN, AccountType.SDN, AccountType.CTDS, AccountType.SREIMB, AccountType.SREIMBCN) + jvList
            }
            docType == null && accMode == AccMode.AP -> {
                listOf(AccountType.PINV, AccountType.PCN, AccountType.PDN, AccountType.PAY, AccountType.VTDS, AccountType.PREIMB, AccountType.EXP) + jvList
            }
            docType == null && accMode == null -> { listOf(AccountType.SINV, AccountType.PINV, AccountType.SCN, AccountType.PCN, AccountType.EXP) }
            else -> { emptyList() }
        }
    }

    /**
     * This private function is used to calculate total settled tds and outputs the amount in document currency
     * @param doc
     * @return BigDecimal
     */
    private suspend fun calculateSettledTds(doc: com.cogoport.ares.api.settlement.entity.Document): BigDecimal {
        return if (!doc.tdsCurrency.isNullOrBlank() && (doc.currency != doc.tdsCurrency)) {
            if (doc.ledCurrency == doc.tdsCurrency) {
                getExchangeValue(doc.settledTds, doc.exchangeRate, true)
            } else {
                val rate = fetchRateSettledTds(doc)
                getExchangeValue(doc.settledTds, rate)
            }
        } else {
            doc.settledTds
        }
    }

    /**
     * This is a private function to fetch exchange rate from the payments
     * @param tdsType
     * @param paymentIds
     * @return List<PaymentData>
     */
    private suspend fun getExchangeRateUsingPayment(
        tdsType: SettlementType?,
        paymentIds: List<Long>
    ): List<PaymentData> {
        val type = mutableListOf<SettlementType>()
        when (tdsType) {
            SettlementType.CTDS -> type.addAll(
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.SINV)
            )
            SettlementType.VTDS -> type.addAll(
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)
            )
            else -> tdsType?.let { type.add(it) }
        }
        return accountUtilizationRepository.getPaymentDetails(paymentIds, type)
    }

    /**
     * This is a private function to fetch exchange rate to calculate settled tds
     * @param doc
     * @return BigDecimal
     */
    private suspend fun fetchRateSettledTds(doc: com.cogoport.ares.api.settlement.entity.Document): BigDecimal {
        val rate = doc.sourceId?.let {
            val rateList = getExchangeRateUsingPayment(doc.sourceType, listOf(it))
            if (rateList.isNotEmpty()) rateList[0].exchangeRate else null
        }

        if (rate == null) {
            doc.tdsCurrency?.let { it ->
                settlementServiceHelper.getExchangeRate(
                    it, doc.currency,
                    SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc.documentDate)
                )
            }
        }
        return rate ?: BigDecimal.ZERO
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
        logger().info("ares.settlement.crossTradeParty: {}", crossTradeParty)
        if (request.entityCode == null) throw AresException(AresError.ERR_1003, "entityCode")
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
                "%${request.query}%",
                request.sortBy,
                request.sortType
            )
        if (documentEntity.isEmpty())
            return ResponseList()
        val tradePartyMappingIds = documentEntity
            .filter { document -> document!!.mappingId != null }
            .map { document -> document!!.mappingId.toString() }
            .distinct()
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
        // Fetch Organization Tds Profile from mappingIds
        val tdsProfiles = listOrgTdsProfile(tradePartyMappingIds)
        for (doc in documentModel) {
            val tdsProfile = tdsProfiles.find { it.id == doc.mappingId }
            val rate = getTdsRate(tdsProfile)
            // Fetch Rate From Profile
            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType), doc.signFlag, doc.accMode)
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.balanceAmount,
                docType = SettlementType.valueOf(doc.accountType)
            )
            if (doc.accMode != AccMode.AP) {
                doc.tds = calculateTds(
                    rate = rate,
                    settledTds = doc.settledTds!!,
                    taxableAmount = doc.taxableAmount
                )
            }
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
        }
        documentModel.forEach { it.documentNo = Hashids.encode(it.documentNo.toLong()) }
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

    override suspend fun check(request: CheckRequest): CheckResponse {
        val stack = runSettlement(request, performDbOperation = false, isAutoKnockOff = false)
        val canSettle = getCanSettleFlag(stack)
        return CheckResponse(
            stackDetails = stack,
            canSettle = canSettle
        )
    }

    private fun getCanSettleFlag(stack: List<CheckDocument>): Boolean {
        var canSettle = true
        stack.forEach {
            if (it.balanceAmount < BigDecimal.ZERO) canSettle = false
            if (it.nostroAmount?.compareTo(BigDecimal.ZERO) != 0) canSettle = false
        }
        return canSettle
    }

    override suspend fun editCheck(request: CheckRequest): CheckResponse {
        adjustBalanceAmount(type = "add", documents = request.stackDetails!!)
        val checkResponse = check(request)
        adjustBalanceAmount(type = "subtract", documents = request.stackDetails!!)
        return checkResponse
    }

    override suspend fun settleWrapper(request: CheckRequest, isAutoKnockOff: Boolean): List<CheckDocument> {
        val settledDocuments = settle(request, isAutoKnockOff)

        val salesInvoiceDocuments = settledDocuments.filter { it.accountType.name == AccountType.SINV.name && it.tds!!.toInt() > 0 }

        when (salesInvoiceDocuments.isNotEmpty()) {
            true -> salesInvoiceDocuments.map { saleInvoice ->
                val paymentId = settlementRepository.getPaymentIdByDestinationIdAndType(Hashids.decode(saleInvoice.documentNo)[0], saleInvoice.accountType, SettlementType.CTDS)
                paymentId?.forEach {
                    onAccountService.directFinalPostToSage(
                        PostPaymentToSage(
                            it,
                            request.createdBy!!
                        )
                    )
                }
            }
        }

        return settledDocuments
    }

    @Transactional
    override suspend fun settle(request: CheckRequest, isAutoKnockOff: Boolean): List<CheckDocument> {
        return runSettlement(request, true, isAutoKnockOff)
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun edit(request: CheckRequest): List<CheckDocument> = editSettlement(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun editTds(request: EditTdsRequest) = editInvoiceTds(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun delete(request: DeleteSettlementRequest) =
        deleteSettlement(request.documentNo, request.settlementType, request.deletedBy, request.deletedByUserType)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun sendForApproval(request: CreateIncidentRequest): String {
        val docList = request.stackDetails!!.map {
            documentConverter.convertToIncidentModel(it)
        }
        val formatedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(request.settlementDate)
        val res = createIncidentMapping(
            accUtilIds = docList.map { Hashids.decode(it.id)[0] },
            data = docList,
            type = com.cogoport.ares.api.common.enums.IncidentType.SETTLEMENT_APPROVAL,
            status = IncidentStatus.REQUESTED,
            orgName = request.orgName,
            entityCode = request.entityCode,
            performedBy = request.createdBy
        )
        val incidentData =
            IncidentData(
                organization = Organization(
                    id = request.orgId,
                    businessName = request.orgName,
                    tradePartyType = null,
                    tradePartyName = null
                ),
                settlementRequest = com.cogoport.hades.model.incident.Settlement(
                    entityCode = request.entityCode!!,
                    list = docList,
                    settlementDate = java.sql.Date.valueOf(formatedDate),
                    incidentMappingId = res,
                    supportingDocUrl = request.supportingDocUrl
                ),
                tdsRequest = null,
                bankRequest = null,
                creditNoteRequest = null
            )
        hadesClient.createIncident(
            com.cogoport.hades.model.incident.request.CreateIncidentRequest(
                type = IncidentType.SETTLEMENT_APPROVAL,
                description = "Settlement Approval For Cross Currency Settle",
                data = incidentData,
                createdBy = request.createdBy!!
            )
        )

        return res
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun reject(request: RejectSettleApproval): String {
        incidentMappingsRepository.updateStatus(
            incidentMappingId = Hashids.decode(request.incidentMappingId!!)[0],
            status = IncidentStatus.REJECTED
        )
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = com.cogoport.hades.model.incident.enums.IncidentStatus.REJECTED,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId!!
        )
        return request.incidentId!!
    }

    /**
     * Get Organization summary orgName, outstanding, currency, tdsStyle.
     * Used to display information on header of TDS Settlement.
     * @param: orgId
     * @param: startDate
     * @param: endDate
     * @return: endDate
     */
    override suspend fun getOrgSummary(request: OrgSummaryRequest): OrgSummaryResponse {
        val responseEntity =
            accountUtilizationRepository.getOrgSummary(request.orgId!!, request.accMode!!, request.startDate, request.endDate)
                ?: throw AresException(AresError.ERR_1005, "")
        val responseModel = orgSummaryConverter.convertToModel(responseEntity)
        val tdsResponse = getSelfOrgTdsProfile(request.orgId!!)
        val tdsStyle = TdsStyle(
            style = tdsResponse.tdsDeductionStyle,
            rate = tdsResponse.tdsDeductionRate,
            type = tdsResponse.tdsDeductionType
        )
        responseModel.tdsStyle = tdsStyle
        return responseModel
    }

    private suspend fun editInvoiceTds(request: EditTdsRequest): String {
        request.documentNo = Hashids.decode(request.documentNo!!)[0].toString()
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
                ?: throw AresException(AresError.ERR_1503, AresConstants.PAYMENT)
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
            return Hashids.encode(tdsDoc.destinationId)
        } else if (currNewTds < tdsDoc.amount) {
            val invoiceTdsDiff = request.oldTds!! - request.newTds!!
            val invoiceTdsDiffLed = Utilities.binaryOperation(invoiceTdsDiff, request.exchangeRate!!, Operator.MULTIPLY)
            val paymentTdsDiff = tdsDoc.amount!! - currNewTds

            reduceAccountUtilization(
                docId = tdsDoc.destinationId,
                accType = AccountType.valueOf(tdsDoc.destinationType.toString()),
                amount = invoiceTdsDiff,
                ledAmount = invoiceTdsDiffLed,
                updatedBy = request.updatedBy!!,
                updatedByUserType = request.updatedByUserType,
                tdsPaid = invoiceTdsDiff
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
            sourceDoc.updatedBy = request.updatedBy
            sourceDoc.updatedAt = Timestamp.from(Instant.now())
            settlementRepository.update(sourceDoc)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.SETTLEMENT,
                    objectId = sourceDoc.id,
                    actionName = AresConstants.UPDATE,
                    data = sourceDoc,
                    performedBy = request.updatedBy.toString(),
                    performedByUserType = request.updatedByUserType
                )
            )
        }
        tdsDoc.amount = currNewTds
        tdsDoc.ledAmount =
            Utilities.binaryOperation(currNewTds, sourceLedgerRate, Operator.MULTIPLY)
        tdsDoc.updatedBy = request.updatedBy
        tdsDoc.updatedAt = Timestamp.from(Instant.now())
        settlementRepository.update(tdsDoc)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.SETTLEMENT,
                objectId = tdsDoc.id,
                actionName = AresConstants.UPDATE,
                data = tdsDoc,
                performedBy = request.updatedBy.toString(),
                performedByUserType = request.updatedByUserType
            )
        )
        return Hashids.encode(tdsDoc.destinationId)
    }

    /**
     * This private suspended function edits the past settlement.
     * @param request
     * @return List<CheckDocument>
     */
    private suspend fun editSettlement(request: CheckRequest): List<CheckDocument> {
        val sourceDoc = getSourceDocFromStack(request.stackDetails!!) ?: throw AresException(AresError.ERR_1501, "")
        deleteSettlement(sourceDoc.documentNo, sourceDoc.accountType, request.createdBy!!, request.createdByUserType)
        return runSettlement(request, true, false)
    }

    /**
     * This private function returns the source document from the stack.
     * @param stack
     * @return CheckDocument?
     */
    private fun getSourceDocFromStack(stack: List<CheckDocument>): CheckDocument? {
        val accTypesInStack = stack.map { it.accountType }
        return if (SettlementType.SINV in accTypesInStack && SettlementType.PINV in accTypesInStack) {
            stack.find { it.accountType == SettlementType.SINV }
        } else {
            stack.find {
                it.accountType in listOf(SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SCN)
            }
        }
    }

    private suspend fun deleteSettlement(documentNo: String, settlementType: SettlementType, deletedBy: UUID, deletedByUserType: String?): String {
        val documentNo = Hashids.decode(documentNo)[0]

        when (settlementType) {
            SettlementType.VTDS -> {
                val sourceDocument = journalVoucherRepository.findById(documentNo)
                if (sourceDocument != null) {
                    if (sourceDocument.status == JVStatus.POSTED) {
                        throw AresException(AresError.ERR_1552, "")
                    }
                }
            }
            SettlementType.CTDS -> {
                val sourceDoc = paymentRepository.findByPaymentNumAndPaymentCode(documentNo, PaymentCode.valueOf(settlementType.name))

                if (sourceDoc != null) {
                    if (sourceDoc.paymentDocumentStatus in listOf(PaymentDocumentStatus.POSTED, PaymentDocumentStatus.FINAL_POSTED)) {
                        throw AresException(AresError.ERR_1552, "")
                    }
                }
            }
            SettlementType.PAY -> {
                val paymentId = paymentRepo.findByPaymentNumAndPaymentCode(documentNo, PaymentCode.PAY)?.id
                if (invoicePaymentMappingRepo.findByPaymentIdFromPaymentInvoiceMapping(paymentId) != 0L) {
                    throw AresException(AresError.ERR_1515, "")
                }
            }
        }

        val sourceType = when (settlementType) {
            SettlementType.REC -> listOf(SettlementType.REC, SettlementType.CTDS, SettlementType.SECH, SettlementType.NOSTRO)
            SettlementType.PAY -> listOf(SettlementType.PAY, SettlementType.VTDS, SettlementType.PECH, SettlementType.NOSTRO)
            SettlementType.SINV -> listOf(SettlementType.SINV, SettlementType.CTDS, SettlementType.VTDS, SettlementType.SECH, SettlementType.PECH, SettlementType.NOSTRO)
            SettlementType.SCN -> listOf(SettlementType.SCN, SettlementType.CTDS, SettlementType.SECH, SettlementType.NOSTRO)
            SettlementType.VTDS -> listOf(SettlementType.VTDS)
            SettlementType.CTDS -> listOf(SettlementType.CTDS)
            else -> if (settlementServiceHelper.getJvList(SettlementType::class.java).contains(settlementType)) {
                settlementServiceHelper.getJvList(SettlementType::class.java).toMutableList().apply {
                    addAll(listOf(SettlementType.SINV, SettlementType.SCN, SettlementType.PINV, SettlementType.PCN, SettlementType.EXP))
                }
            } else {
                listOf(SettlementType.PCN, SettlementType.VTDS, SettlementType.PECH, SettlementType.NOSTRO)
            }
        }

        val fetchedDoc = settlementRepository.findBySourceIdAndSourceType(documentNo, sourceType)
        if (fetchedDoc.any { it!!.settlementStatus == SettlementStatus.POSTED }) {
            throw AresException(AresError.ERR_1544, "")
        }

        val paymentTdsDoc = fetchedDoc.find { it?.destinationId == documentNo }
        val debitDoc = fetchedDoc.filter { it?.destinationId != documentNo }.groupBy { it?.destinationId }
        val sourceCurr = fetchedDoc.sumOf { it?.amount ?: BigDecimal.ZERO }
        reduceAccountUtilization(
            docId = documentNo,
            accType = AccountType.valueOf(settlementType.toString()),
            amount = sourceCurr,
            updatedBy = deletedBy,
            updatedByUserType = deletedByUserType,
            tdsPaid = paymentTdsDoc?.amount
        )
        for (debits in debitDoc) {
            val settledDoc =
                debits.value.filter { it?.sourceType == settlementType }
            val tdsDoc = debits.value.find { it?.sourceType in listOf(SettlementType.CTDS, SettlementType.VTDS) }
            var tdsPaid = tdsDoc?.amount ?: BigDecimal.ZERO
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
                    tdsPaid = getExchangeValue(tdsPaid, rate)
                }
                reduceAccountUtilization(
                    docId = source.destinationId,
                    accType = AccountType.valueOf(source.destinationType.toString()),
                    amount = settledCurr,
                    updatedBy = deletedBy,
                    updatedByUserType = deletedByUserType,
                    tdsPaid = tdsPaid
                )
            }
        }
        val settlements = settlementRepository.findByIdIn(fetchedDoc.map { it?.id!! })
        settlementRepository.deleteByIdIn(fetchedDoc.map { it?.id!! })
        for (settlementDoc in settlements) {
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.SETTLEMENT,
                    objectId = settlementDoc.id,
                    actionName = AresConstants.DELETE,
                    data = settlementDoc,
                    performedBy = deletedBy.toString(),
                    performedByUserType = deletedByUserType
                )
            )
        }

        return Hashids.encode(documentNo)
    }

    @Transactional
    open suspend fun reduceAccountUtilization(
        docId: Long,
        accType: AccountType,
        amount: BigDecimal,
        ledAmount: BigDecimal? = null,
        updatedBy: UUID,
        updatedByUserType: String? = null,
        tdsPaid: BigDecimal? = null
    ) {
        val accUtil =
            accountUtilizationRepository.findRecord(docId, accType.toString())
                ?: throw AresException(AresError.ERR_1503, "${accType}_$docId")
        if ((accUtil.payCurr - amount).compareTo(BigDecimal.ZERO) == 0) {
            accUtil.payCurr = BigDecimal.ZERO
            accUtil.payLoc = BigDecimal.ZERO
        } else {
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
        }
        accUtil.updatedAt = Timestamp.from(Instant.now())
        val accUtilObj = accountUtilizationRepository.update(accUtil)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accUtil.organizationId))

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilObj.id,
                actionName = AresConstants.UPDATE,
                data = accUtilObj,
                performedBy = updatedBy.toString(),
                performedByUserType = updatedByUserType
            )
        )
        val paidTds = (tdsPaid ?: BigDecimal.ZERO) * (-1).toBigDecimal()
        updateExternalSystemInvoice(accUtilObj, paidTds, updatedBy, updatedByUserType, false, true)
        emitDashboardAndOutstandingEvent(accUtilObj)
    }

    @Transactional
    open suspend fun runSettlement(
        request: CheckRequest,
        performDbOperation: Boolean,
        isAutoKnockOff: Boolean
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
                SettlementType.SCN,
                SettlementType.SREIMBCN
            )
        val debitType =
            listOf(
                SettlementType.SINV,
                SettlementType.PINV,
                SettlementType.SDN,
                SettlementType.PDN,
                SettlementType.PREIMB,
                SettlementType.SREIMB,
                SettlementType.EXP
            )
        val jvType = settlementServiceHelper.getJvList(SettlementType::class.java)
        for (doc in request.stackDetails!!.reversed()) {
            if (creditType.contains(doc.accountType)) {
                source.add(doc)
            } else if (debitType.contains(doc.accountType)) {
                dest.add(doc)
            } else if (jvType.contains(doc.accountType)) {
                when (doc.accMode) {
                    AccMode.AR -> {
                        when (doc.signFlag.toInt()) {
                            1 -> dest.add(doc)
                            -1 -> source.add(doc)
                        }
                    }
                    AccMode.AP -> {
                        when (doc.signFlag.toInt()) {
                            1 -> source.add(doc)
                            -1 -> dest.add(doc)
                        }
                    }
                }
            }
        }
        if (source.isEmpty() &&
            (
                dest.map { it.accountType }.contains(SettlementType.SINV) &&
                    dest.map { it.accountType }.contains(SettlementType.PINV)
                ) ||
            (
                dest.map { it.accountType }.contains(SettlementType.SREIMB) &&
                    dest.map { it.accountType }.contains(SettlementType.PREIMB)
                ) || (
                dest.map { it.accountType }.contains(SettlementType.EXP) &&
                    dest.map { it.accountType }.contains(SettlementType.SINV)
                )
        ) {

            val allowedSettlementType = mutableListOf(SettlementType.SREIMB, SettlementType.SINV)
            val res = dest.filter { it -> allowedSettlementType.contains(it.accountType) }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }

        if (dest.isEmpty() && source.any { it.accMode == AccMode.CSD } && (
            source.map { it.accountType }.contains(SettlementType.REC) &&
                source.map { it.accountType }.contains(SettlementType.PAY)
            )
        ) {
            val allowedSettlementType = mutableListOf(SettlementType.PAY)
            source.filter { it -> allowedSettlementType.contains(it.accountType) }.forEach {
                dest.add(it)
                source.remove(it)
            }
        }
        businessValidation(source, dest)
        if (source.any { it.hasPayrun } || dest.any { it.hasPayrun }) {
            throw AresException(AresError.ERR_1512, "")
        }
        val settledList = settleDocuments(request, source, dest, performDbOperation, isAutoKnockOff)
        settledList.forEach {
            it.id = Hashids.encode(it.id.toLong())
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.settledTds = settledTdsCopy[it.id]!!
        }
        return request.stackDetails!!.map { r -> settledList.filter { it.id == r.id }[0] }
    }

    /**
     * Settle documents: source to destination.
     */
    @Transactional
    open suspend fun settleDocuments(
        request: CheckRequest,
        source: MutableList<CheckDocument>,
        dest: MutableList<CheckDocument>,
        performDbOperation: Boolean,
        isAutoKnockOff: Boolean
    ): MutableList<CheckDocument> {
        val response = mutableListOf<CheckDocument>()
        for (payment in source) {
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType)) {
                    availableAmount =
                        doSettlement(
                            request,
                            invoice,
                            availableAmount,
                            payment,
                            source,
                            performDbOperation,
                            isAutoKnockOff
                        )
                }
                if (payment.tds!!.compareTo(BigDecimal.ZERO) != 0 &&
                    payment.settledTds.compareTo(BigDecimal.ZERO) == 0 &&
                    payment.accountType in (listOf(SettlementType.PINV, SettlementType.SINV, SettlementType.EXP)) &&
                    performDbOperation
                ) {
                    createTdsRecord(
                        sourceId = payment.documentNo.toLong(),
                        destId = payment.documentNo.toLong(),
                        destType = payment.accountType,
                        currency = payment.currency,
                        ledCurrency = payment.ledCurrency,
                        tdsAmount = payment.tds!!,
                        tdsLedAmount = getExchangeValue(payment.tds!!, payment.exchangeRate),
                        settlementDate = request.settlementDate,
                        signFlag = 1,
                        createdBy = request.createdBy,
                        createdByUserType = request.createdByUserType,
                        supportingDocUrl = request.supportingDocUrl,
                        exchangeRate = payment.exchangeRate,
                        paymentTransactionDate = payment.transactionDate
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

    @Transactional
    open suspend fun doSettlement(
        request: CheckRequest,
        invoice: CheckDocument,
        availableAmount: BigDecimal,
        payment: CheckDocument,
        source: MutableList<CheckDocument>,
        performDbOperation: Boolean,
        isAutoKnockOff: Boolean
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
                        performDbOperation,
                        isAutoKnockOff
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
                        performDbOperation,
                        isAutoKnockOff
                    )
            }
        }
        return amount
    }

    @Transactional
    open suspend fun updateDocuments(
        request: CheckRequest,
        invoice: CheckDocument,
        payment: CheckDocument,
        toSettleAmount: BigDecimal,
        availableAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        updateDoc: Boolean,
        performDbOperation: Boolean,
        isAutoKnockOff: Boolean
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
        if (performDbOperation && toSettleAmount.compareTo(BigDecimal.ZERO) != 0)
            performDbOperation(
                request,
                toSettleAmount,
                exchangeRate,
                ledgerRate,
                payment,
                invoice,
                isAutoKnockOff
            )
        return getExchangeValue(amount, exchangeRate, true)
    }

    @Transactional
    open suspend fun performDbOperation(
        request: CheckRequest,
        toSettleAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        payment: CheckDocument,
        invoice: CheckDocument,
        isAutoKnockOff: Boolean
    ) {
        val jvList = settlementServiceHelper.getJvList(SettlementType::class.java)

        /** Invoice Amount in payment currency */
        val paidAmount = getExchangeValue(toSettleAmount, exchangeRate, true)
        /** Payment ledger amount */
        val paidLedAmount = getExchangeValue(paidAmount, ledgerRate)
        /** Tds Amount in Invoice currency */
        var invoiceTds = invoice.tds!! - invoice.settledTds
        /** Tds Amount in Invoice ledger currency */
        val invoiceTdsLed = invoiceTds * (invoice.exchangeRate)
        /** Tds Amount in Payment currency */
        val paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        /** Payment Tds ledger Amount */
        val paymentTdsLed = getExchangeValue(paymentTds, ledgerRate)
        /** Nostro Amount in Invoice currency */
        val invoiceNostro = invoice.nostroAmount!! - invoice.settledNostro!!
        /** Nostro Amount in payment currency */
        val paymentNostro = getExchangeValue(invoiceNostro, exchangeRate, true)
        /** Payment Nostro Ledger Amount */
        val paymentNostroLed = getExchangeValue(paymentNostro, ledgerRate)

        val isNotJv = payment.accountType !in jvList

        val amount = paidAmount + if (isNotJv) paymentNostro else BigDecimal.ZERO
        val ledAmount = paidLedAmount + if (isNotJv) paymentNostroLed else BigDecimal.ZERO
        // Create Documents Settlement Entry
        if (amount.compareTo(BigDecimal.ZERO) != 0) {
            createSettlement(
                payment.documentNo.toLong(),
                payment.accountType,
                invoice.documentNo.toLong(),
                invoice.accountType,
                payment.currency,
                amount,
                payment.ledCurrency,
                ledAmount,
                1,
                request.settlementDate,
                request.createdBy,
                request.createdByUserType,
                request.supportingDocUrl
            )
        }
        // Create TDS Entry
        if (invoiceTds.compareTo(BigDecimal.ZERO) != 0 && (isNotJv) && (invoice.ledCurrency != AresConstants.VND)) {
            createTdsRecord(
                sourceId = payment.documentNo.toLong(),
                destId = invoice.documentNo.toLong(),
                destType = invoice.accountType,
                currency = invoice.currency,
                ledCurrency = invoice.ledCurrency,
                tdsAmount = invoiceTds,
                tdsLedAmount = invoiceTdsLed,
                settlementDate = request.settlementDate,
                signFlag = -1,
                createdBy = request.createdBy,
                createdByUserType = request.createdByUserType,
                supportingDocUrl = request.supportingDocUrl,
                exchangeRate = invoice.exchangeRate,
                paymentTransactionDate = payment.transactionDate
            )
            invoice.settledTds += invoiceTds
        }

        // Create Nostro Entry
        if (paymentNostro.compareTo(BigDecimal.ZERO) != 0 && (isNotJv)) {
            createSettlement(
                sourceId = payment.documentNo.toLong(),
                sourceType = SettlementType.NOSTRO,
                destId = invoice.documentNo.toLong(),
                destType = invoice.accountType,
                currency = payment.currency,
                amount = paymentNostro,
                ledCurrency = payment.ledCurrency,
                ledAmount = paymentNostroLed,
                signFlag = -1,
                transactionDate = request.settlementDate,
                createdBy = request.createdBy,
                createdByUserType = request.createdByUserType,
                supportingDocUrl = request.supportingDocUrl
            )
            invoice.settledNostro = invoice.settledNostro!! + invoiceNostro
        }

        // Create Exchange Loss Gain Entry
        if (payment.ledCurrency != invoice.currency) {
            val excLedAmount =
                getExchangeValue(toSettleAmount, invoice.exchangeRate) - (paidLedAmount)
            if (excLedAmount.compareTo(BigDecimal.ZERO) != 0) {
                val exType =
                    if (invoice.accMode == AccMode.AR)
                        SettlementType.SECH
                    else SettlementType.PECH
                val exSign =
                    excLedAmount.signum() *
                        if (payment.accountType in listOf(
                                SettlementType.SCN,
                                SettlementType.REC,
                                SettlementType.SINV
                            )
                        ) {
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
                    request.createdBy,
                    request.createdByUserType,
                    request.supportingDocUrl
                )
            }
        }

        // Update Documents in Account Utilization
        val utilizedTdsOfPaymentDoc =
            if (payment.accountType in listOf(SettlementType.SINV, SettlementType.PCN, SettlementType.SCN))
                (payment.tds ?: BigDecimal.ZERO) - payment.settledTds
            else BigDecimal.ZERO

        val paymentUtilized = (paidAmount + utilizedTdsOfPaymentDoc).setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.DOWN)
        val invoiceUtilized = toSettleAmount + if (isNotJv) invoiceTds + invoiceNostro else BigDecimal.ZERO
        updateAccountUtilization(payment, paymentUtilized, utilizedTdsOfPaymentDoc, request.createdBy!!, request.createdByUserType, isAutoKnockOff) // Update Payment
        updateAccountUtilization(invoice, invoiceUtilized, invoiceTds, request.createdBy!!, request.createdByUserType, isAutoKnockOff) // Update Invoice
    }

    @Transactional
    open suspend fun createTdsRecord(
        sourceId: Long?,
        destId: Long,
        destType: SettlementType,
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        signFlag: Short,
        settlementDate: Timestamp,
        createdBy: UUID?,
        createdByUserType: String?,
        supportingDocUrl: String?,
        exchangeRate: BigDecimal?,
        paymentTransactionDate: Date
    ) {
        val invoiceAndBillData = accountUtilizationRepository.findRecord(destId, destType.toString())

        var updatedSourceId = sourceId
        var updatedSourceType: SettlementType?

        if (invoiceAndBillData?.accMode == AccMode.AP) {
            updatedSourceId = createTdsAsJv(
                currency,
                ledCurrency,
                tdsAmount,
                tdsLedAmount,
                createdBy,
                createdByUserType,
                invoiceAndBillData,
                exchangeRate,
                paymentTransactionDate,
                utr = "TDS AGAINST $destId",
                tdsAmount,
                tdsLedAmount
            )
            updatedSourceType = SettlementType.VTDS
        } else {
            updatedSourceId = createTdsAsPaymentEntry(
                destId,
                destType,
                currency,
                ledCurrency,
                tdsAmount,
                tdsLedAmount,
                createdBy,
                createdByUserType,
                SettlementType.CTDS,
                invoiceAndBillData,
                exchangeRate,
                paymentTransactionDate
            )
            updatedSourceType = SettlementType.CTDS
        }

        createSettlement(
            updatedSourceId,
            updatedSourceType,
            destId,
            destType,
            currency,
            tdsAmount,
            ledCurrency,
            tdsLedAmount,
            signFlag,
            settlementDate,
            createdBy,
            createdByUserType,
            supportingDocUrl
        )
    }

    @Transactional
    open suspend fun updateAccountUtilization(
        document: CheckDocument,
        utilizedAmount: BigDecimal,
        paidTds: BigDecimal,
        updatedBy: UUID,
        updatedByUserType: String?,
        isAutoKnockOff: Boolean
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
        if ((paymentUtilization.amountCurr - paymentUtilization.payCurr) < utilizedAmount.setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.DOWN)) {
            throw AresException(AresError.ERR_1504, " Document No: ${paymentUtilization.documentValue}")
        }

        paymentUtilization.payCurr += utilizedAmount
        paymentUtilization.payLoc += getExchangeValue(utilizedAmount, document.exchangeRate)
        if (paymentUtilization.accMode == AccMode.AR) {
            paymentUtilization.tdsAmount = paymentUtilization.tdsAmount!! + paidTds
            paymentUtilization.tdsAmountLoc = paymentUtilization.tdsAmountLoc!! + getExchangeValue(paidTds, document.exchangeRate)
        }
        paymentUtilization.updatedAt = Timestamp.from(Instant.now())

        if (paymentUtilization.amountCurr.subtract(paymentUtilization.payCurr).abs() <= BigDecimal(0.001)) {
            paymentUtilization.payCurr = paymentUtilization.amountCurr
        }

        if (paymentUtilization.amountLoc.subtract(paymentUtilization.payLoc).abs() <= BigDecimal(0.001)) {
            paymentUtilization.payLoc = paymentUtilization.amountLoc
        }
        val accountUtilization = accountUtilizationRepository.update(paymentUtilization)
        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(paymentUtilization.organizationId))

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accountUtilization.id,
                actionName = AresConstants.UPDATE,
                data = accountUtilization,
                performedBy = updatedBy.toString(),
                performedByUserType = updatedByUserType
            )
        )
        updateExternalSystemInvoice(accountUtilization, paidTds, updatedBy, updatedByUserType, isAutoKnockOff)
        emitDashboardAndOutstandingEvent(paymentUtilization)
    }

    /**
     * Invokes Kafka for Plutus(Sales) or Kuber(Purchase) based on accountType in accountUtilization.
     * @param: accountUtilization
     */
    @Transactional
    open suspend fun updateExternalSystemInvoice(
        accountUtilization: AccountUtilization,
        paidTds: BigDecimal,
        performedBy: UUID,
        performedByUserType: String?,
        isAutoKnockOff: Boolean = false,
        isDelete: Boolean = false
    ) {
        if (settlementServiceHelper.getJvList(AccountType::class.java).contains(accountUtilization.accType)) {
            journalVoucherService.updateJournalVoucherStatus(
                id = accountUtilization.documentNo,
                documentValue = accountUtilization.documentValue,
                isUtilized = accountUtilization.payCurr.compareTo(BigDecimal.ZERO) != 0,
                performedBy = performedBy,
                performedByUserType = performedByUserType
            )
        }
        when (accountUtilization.accType) {
            AccountType.PINV, AccountType.PCN, AccountType.EXP -> emitPayableBillStatus(accountUtilization, paidTds, performedBy, performedByUserType, isAutoKnockOff, isDelete)
            AccountType.SINV, AccountType.SCN -> updateBalanceAmount(accountUtilization, performedBy, performedByUserType)
            else -> {}
        }
    }

    /**
     * Invokes Kafka event to update balanceAmount in Plutus(Sales MS).
     * @param: accountUtilization
     */
    @Transactional
    open suspend fun updateBalanceAmount(
        accountUtilization: AccountUtilization,
        performedBy: UUID,
        performedByUserType: String?
    ) {

        var knockOffDocuments: List<PaymentInfoRec>? = null
        if (accountUtilization.accType == AccountType.SINV)
            knockOffDocuments = knockOffListData(accountUtilization)

        val paymentStatus = Utilities.getPaymentStatus(accountUtilization)

        plutusMessagePublisher.emitInvoiceBalance(
            invoiceBalanceEvent = UpdateInvoiceBalanceEvent(
                invoiceBalance = InvoiceBalance(
                    invoiceId = accountUtilization.documentNo,
                    balanceAmount = paymentStatus.second,
                    performedBy = performedBy,
                    performedByUserType = performedByUserType,
                    paymentStatus = paymentStatus.first
                ),
                knockOffDocuments = knockOffDocuments

            )
        )
    }

    private suspend fun knockOffListData(accountUtilization: AccountUtilization): List<PaymentInfoRec>? {

        val listOfKnockOffData: MutableList<PaymentInfoRec> = mutableListOf()

        var listOfSourceId = settlementRepository.getSettlementDetails(accountUtilization.documentNo)
        if (listOfSourceId != null) {
            listOfKnockOffData.addAll(settlementRepository.getPaymentDetailsInRec(listOfSourceId))
            listOfKnockOffData.addAll(settlementRepository.getKnockOffDocument(listOfSourceId))
        }
        return listOfKnockOffData
    }

    /**
     * Invokes Kafka event to update status in Kuber(Purchase MS).
     * @param: accountUtilization
     */
    @Transactional
    open suspend fun emitPayableBillStatus(
        accountUtilization: AccountUtilization,
        paidTds: BigDecimal,
        performedBy: UUID?,
        performedByUserType: String?,
        isAutoKnockOff: Boolean,
        isDelete: Boolean
    ) {
        val status = if (accountUtilization.payLoc.compareTo(BigDecimal.ZERO) == 0)
            "UNPAID"
        else if ((accountUtilization.amountCurr - accountUtilization.tdsAmount!!) > accountUtilization.payCurr)
            "PARTIAL"
        else
            "FULL"

        val paymentInfo = if (accountUtilization.accType == AccountType.PCN) {
            val settlementInfo = settlementRepository.getSettlementDateBySourceId(accountUtilization.documentNo)
            PaymentInfo(
                entityCode = null,
                bankId = null,
                bankName = null,
                transRefNumber = null,
                payMode = null,
                settlementDate = settlementInfo.settlementDate,
                settlementNum = settlementInfo.settlementNum
            )
        } else {
            settlementRepository.getPaymentDetailsByPaymentNum(accountUtilization.documentNo)
        }
        kuberMessagePublisher.emitUpdateBillPaymentStatus(
            UpdatePaymentStatusRequest(
                billId = accountUtilization.documentNo,
                paymentStatus = status,
                organizationName = accountUtilization.organizationName,
                paidAmount = accountUtilization.payCurr,
                performedBy = performedBy,
                performedByUserType = performedByUserType,
                isAutoKnockOff = isAutoKnockOff,
                transferMode = if (paymentInfo?.payMode == "CHQ") "CHEQUE" else paymentInfo?.payMode,
                transactionRef = paymentInfo?.transRefNumber,
                cogoBankId = paymentInfo?.bankId.toString(),
                cogoBankName = paymentInfo?.bankName,
                cogoEntity = paymentInfo?.entityCode,
                paymentDate = paymentInfo?.settlementDate,
                paidTds = paidTds,
                settlementNum = paymentInfo?.settlementNum!!,
                deleteSettlement = isDelete
            )
        )
        aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accountUtilization.organizationId))
    }

    private suspend fun emitDashboardAndOutstandingEvent(
        accUtilizationRequest: AccountUtilization
    ) {
        if (accUtilizationRequest.accMode == AccMode.AR) {
            emitOutstandingData(accUtilizationRequest)
        }
    }

    private suspend fun emitOutstandingData(accUtilizationRequest: AccountUtilization) {
        aresMessagePublisher.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    orgId = accUtilizationRequest.organizationId.toString(),
                    orgName = accUtilizationRequest.organizationName,
                    serviceType = if (accUtilizationRequest.serviceType.isNullOrBlank()) null else ServiceType.valueOf(accUtilizationRequest.serviceType.uppercase()),
                    invoiceCurrency = accUtilizationRequest.currency
                )
            )
        )
    }

    @Transactional
    open suspend fun createSettlement(
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
        createdBy: UUID?,
        createdByUserType: String?,
        supportingDocUrl: String?
    ) {
        val settlementStatus = if (listOf(SettlementType.SECH, SettlementType.PECH).contains(sourceType)) {
            SettlementStatus.POSTED
        } else {
            SettlementStatus.CREATED
        }

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
                Timestamp.from(Instant.now()),
                supportingDocUrl,
                false,
                sequenceGeneratorImpl.getSettlementNumber(),
                settlementStatus
            )
        val settleDoc = settlementRepository.save(settledDoc)

//        aresMessagePublisher.emitUnfreezeCreditConsumption(settleDoc)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.SETTLEMENT,
                objectId = settleDoc.id,
                actionName = AresConstants.CREATE,
                data = settleDoc,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )
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
        val jvSettleList = listOf(SettlementType.SINV, SettlementType.PINV, SettlementType.REC, SettlementType.PAY, SettlementType.SREIMB, SettlementType.PREIMB, SettlementType.SCN, SettlementType.EXP)
        val jvList = settlementServiceHelper.getJvList(classType = SettlementType::class.java)

        if (jvList.contains(accType)) {
            return jvSettleList + jvList
        }

        return when (accType) {
            SettlementType.REC -> {
                listOf(SettlementType.SINV, SettlementType.SDN, SettlementType.PAY) + jvList
            }
            SettlementType.PINV -> {
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV) + jvList
            }
            SettlementType.EXP -> {
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV) + jvList
            }
            SettlementType.PCN -> {
                listOf(SettlementType.PINV, SettlementType.PDN, SettlementType.EXP) + jvList
            }
            SettlementType.PAY -> {
                listOf(SettlementType.PINV, SettlementType.PDN, SettlementType.EXP, SettlementType.REC) + jvList
            }
            SettlementType.SINV -> {
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.PINV) + jvList
            }
            SettlementType.SCN -> {
                listOf(SettlementType.SINV, SettlementType.SDN) + jvList
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
                listOf(SettlementType.PINV, SettlementType.PDN, SettlementType.PCN, SettlementType.EXP)
            }
            SettlementType.PREIMB -> {
                listOf(SettlementType.SREIMB)
            }
            SettlementType.SREIMB -> {
                listOf(SettlementType.PREIMB)
            }
            SettlementType.SREIMBCN -> {
                listOf(SettlementType.SREIMB)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun storeSettledTds(request: CheckRequest): MutableMap<String, BigDecimal> {
        val settledTdsCopy = mutableMapOf<String, BigDecimal>()
        request.stackDetails!!.forEach {
            settledTdsCopy[it.id] = it.settledTds
        }
        return settledTdsCopy
    }

    private fun sanitizeInput(request: CheckRequest) {
        request.stackDetails!!.forEach {
            it.id = Hashids.decode(it.id)[0].toString()
            it.documentNo = Hashids.decode(it.documentNo)[0].toString()
            it.settledAllocation = BigDecimal.ZERO
            it.settledNostro = BigDecimal.ZERO
            it.settledTds = BigDecimal.ZERO
            if (it.nostroAmount == null) it.nostroAmount = BigDecimal.ZERO
            if (it.tds == null) it.tds = BigDecimal.ZERO
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
            documents.forEach {
                it.balanceAmount += (it.settledAmount) ?: BigDecimal.ZERO
            }
        }
        if (type == "subtract") {
            documents.forEach {
                it.balanceAmount -= (it.settledAmount) ?: BigDecimal.ZERO
            }
        }
        return documents
    }

    private suspend fun createIncidentMapping(
        accUtilIds: List<Long>?,
        data: Any?,
        type: com.cogoport.ares.api.common.enums.IncidentType?,
        status: IncidentStatus?,
        orgName: String?,
        entityCode: Int?,
        performedBy: UUID?
    ): String {
        val incMapObj = IncidentMappings(
            id = null,
            accountUtilizationIds = accUtilIds,
            data = data,
            incidentType = type,
            incidentStatus = status,
            organizationName = orgName,
            entityCode = entityCode,
            createdBy = performedBy,
            updatedBy = performedBy,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now())
        )
        val savedObj = incidentMappingsRepository.save(incMapObj)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.INCIDENT_MAPPINGS,
                objectId = savedObj.id,
                actionName = AresConstants.CREATE,
                data = savedObj,
                performedBy = performedBy.toString(),
                performedByUserType = null
            )
        )
        return Hashids.encode(savedObj.id!!)
    }

    override suspend fun settleWithSourceIdAndDestinationId(
        autoKnockOffRequest: AutoKnockOffRequest
    ): List<CheckDocument>? {
        val decodedSourceId = Hashids.decode(autoKnockOffRequest.paymentIdAsSourceId)[0]
        val decodedDestinationId = Hashids.decode(autoKnockOffRequest.destinationId)[0]
        val sourceDocumentNo = if (listOf("PAY", "REC", "CTDS").contains(autoKnockOffRequest.sourceType!!)) {
            paymentRepo.findByPaymentId(decodedSourceId).paymentNum!!
        } else {
            decodedSourceId
        }
        val listOfDocuments = accountUtilizationRepo.getAccUtilFromDocNumbers(listOf(sourceDocumentNo, decodedDestinationId), listOf(autoKnockOffRequest.sourceType, autoKnockOffRequest.destinationType))

        if (listOfDocuments.isNullOrEmpty()) return null

        val docValues: List<String> = listOfDocuments.map { it.documentValue!! }

        val documents = getDocumentList(SettlementDocumentRequest(docValues = docValues, countRequired = false))

        val checkDocumentData = documents.list.map {
            documentConverter.convertDocumentModelToCheckDocument(it!!)
        } as MutableList<CheckDocument>

        val checkRequest = CheckRequest(
            stackDetails = checkDocumentData,
            createdBy = autoKnockOffRequest.createdBy,
            createdByUserType = null,
            incidentId = null,
            incidentMappingId = null,
            remark = null
        )

        return settle(checkRequest)
    }

    private suspend fun calculatingTds(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>, entityCode: Int?): List<Document> {
        val documentModel = groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        documentModel.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = Hashids.encode(it.id.toLong())
        }
        val tradePartyMappingIds = documentEntity
            .filter { document -> document!!.mappingId != null }
            .map { document -> document!!.mappingId.toString() }
            .distinct()
        val tdsProfiles = listOrgTdsProfile(tradePartyMappingIds)

        for (doc in documentModel) {
            val tdsProfile = tdsProfiles.find { it.id == doc.mappingId }
            val rate = getTdsRate(tdsProfile)
            if (doc.accMode != AccMode.AP) {
                doc.tds = when (doc.accountType in listOf(AccountType.SINV.name) && entityCode != AresConstants.ENTITY_501 && doc.migrated == false) {
                    true -> calculateTds(
                        rate = rate,
                        settledTds = doc.settledTds!!,
                        taxableAmount = doc.taxableAmount
                    )
                    else -> BigDecimal.ZERO
                }
            }
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType), doc.signFlag, doc.accMode)
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.currentBalance,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.settledAllocation = BigDecimal.ZERO
            doc.allocationAmount = doc.balanceAmount
            doc.balanceAfterAllocation = BigDecimal.ZERO
        }

        return documentModel
    }

    override suspend fun sendKnockOffDataToCreditConsumption(request: Settlement) {
        val invoiceData = plutusClient.getInvoiceAdditionalByInvoiceId(request.destinationId, "paymentMode")

        if ((invoiceData == null) || invoiceData.value.toString().isBlank() || (invoiceData.value != TransactionType.CREDIT.value)) {
            return
        }

        var transRefNumber: String? = null
        if (request.sourceType == SettlementType.REC) {
            transRefNumber = paymentRepo.findTransRefNumByPaymentNum(request.sourceId)
        }

        val destinationDocument = accountUtilizationRepository.findRecord(request.destinationId, request.destinationType.name)

        val payCurrency = "INR"
        val transactionType = TransactionType.CREDIT.value

        val paymentRequest = com.cogoport.plutus.model.invoice.CreditPaymentRequest(
            organizationId = destinationDocument?.taggedOrganizationId,
            invoiceNumber = destinationDocument?.documentValue!!,
            invoiceDate = invoiceData.invoiceDate.toString(),
            invoiceDueDate = destinationDocument.dueDate.toString(),
            invoiceAmount = destinationDocument.amountLoc,
            paidAmount = request.amount,
            transactionType = transactionType,
            currency = payCurrency,
            proformaNumber = invoiceData.proformaNumber,
            paymentDate = request.settlementDate.toString(),
            triggerSource = "knockoff_invoked",
            transactionDetails = com.cogoport.plutus.model.invoice.TransactionDetails(
                utrNumber = transRefNumber,
                irnNumber = null,
                referenceProformaNumber = null,
                transactionDocuments = arrayListOf(
                    TransactionDocuments(
                        type = invoiceData.invoiceType.name,
                        url = invoiceData.invoicePdfUrl
                    )
                )
            ),
            organizationTradePartyId = destinationDocument.tradePartyMappingId
        )

        val objectName = "UNFREEZE_CREDIT"
        payLaterUtilizationAndKnockOffCall(paymentRequest, request.destinationId, objectName)
    }

    override suspend fun sendInvoiceDataToDebitConsumption(request: AccountUtilization) {
        val invoiceData = plutusClient.getInvoiceAdditionalByInvoiceId(request.documentNo, "paymentMode")

        if ((invoiceData == null) || invoiceData.value.toString().isBlank() || (invoiceData.value != TransactionType.CREDIT.value)) {
            return
        }

        val destinationDocument = accountUtilizationRepository.findRecord(request.documentNo, request.accType.name, request.accMode.name)

        val paymentRequest = com.cogoport.plutus.model.invoice.CreditPaymentRequest(
            organizationId = destinationDocument?.taggedOrganizationId,
            invoiceNumber = destinationDocument?.documentValue!!,
            invoiceDate = invoiceData.invoiceDate.toString(),
            invoiceDueDate = destinationDocument.dueDate.toString(),
            invoiceAmount = destinationDocument.amountLoc,
            paidAmount = request.payLoc * BigDecimal.valueOf(-1),
            transactionType = TransactionType.DEBIT.value,
            currency = "INR",
            proformaNumber = invoiceData.proformaNumber,
            triggerSource = "knockoff_reverted",
            paymentDate = null,
            transactionDetails = com.cogoport.plutus.model.invoice.TransactionDetails(
                utrNumber = null,
                irnNumber = null,
                referenceProformaNumber = null,
                transactionDocuments = arrayListOf(
                    TransactionDocuments(
                        type = invoiceData.invoiceType.name,
                        url = invoiceData.invoicePdfUrl
                    )
                )
            ),
            organizationTradePartyId = destinationDocument.tradePartyMappingId
        )

        val objectName = "UNFREEZE_CREDIT_DELETED"
        payLaterUtilizationAndKnockOffCall(paymentRequest, request.documentNo, objectName)
    }

    private suspend fun payLaterUtilizationAndKnockOffCall(request: com.cogoport.plutus.model.invoice.CreditPaymentRequest, invoiceId: Long, objectName: String) {
        var response: String? = ""
        response = try {
            railsClient.sendInvoicePaymentKnockOff(request)
        } catch (e: Exception) {
            logger().error(e.toString())
            e.localizedMessage.toString()
        }

        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                id = null,
                apiName = "CreditConsumption",
                apiType = "On_Credit",
                objectId = invoiceId,
                objectName = objectName,
                httpResponseCode = "",
                requestParams = request.toString(),
                response = response.toString(),
                isSuccess = true
            )
        )
    }

    override suspend fun bulkMatchingSettlementOnSage(settlementIds: List<Long>, performedBy: UUID) {
        settlementIds.forEach {
            aresMessagePublisher.emitBulkMatchingSettlementOnSage(
                PostSettlementRequest(
                    settlementId = it,
                    performedBy = performedBy
                )
            )
        }
    }

    override suspend fun matchingOnSage(settlementIds: List<Long>, performedBy: UUID): SageFailedResponse {
        val failedIds: MutableList<Long?> = mutableListOf()
        settlementIds.forEach {
            val settlementResponse = matchingSettlementOnSage(it, performedBy)
            if (!settlementResponse) {
                failedIds.add(it)
            }
        }
        return SageFailedResponse(
            failedIdsList = failedIds
        )
    }

    override suspend fun matchingSettlementOnSage(settlementId: Long, performedBy: UUID): Boolean {

        val listOfRecOrPayCode = listOf(AccountType.PAY, AccountType.REC, AccountType.CTDS, AccountType.VTDS)
        try {
            // Fetch source and destination details
            val settlement = settlementRepository.findById(settlementId) ?: throw AresException(AresError.ERR_1002, "Settlement for this Id")
            if (settlement.settlementStatus == SettlementStatus.POSTED) {
                return true
            }

            // Fetch source details
            val sourceDocument = accountUtilizationRepository.findByDocumentNo(settlement.sourceId!!, AccountType.valueOf(settlement.sourceType.toString()))

            // Fetch destination details
            val destinationDocument = accountUtilizationRepository.findByDocumentNo(settlement.destinationId, AccountType.valueOf(settlement.destinationType.toString()))

            if (destinationDocument.isProforma == true) {
                throw AresException(AresError.ERR_1547, "")
            }
            if (sourceDocument.isProforma == true) {
                throw AresException(AresError.ERR_1547, "")
            }

            val sageOrganizationResponse = checkIfOrganizationIdIsValid(settlementId, sourceDocument.accMode, sourceDocument)
            val sourcePresentOnSage = if (sourceDocument.migrated == true && listOf(AccountType.REC, AccountType.CTDS, AccountType.VTDS, AccountType.PAY).contains(sourceDocument.accType)) {
                paymentRepo.findBySinglePaymentNumValue(sourceDocument.documentValue!!)
            } else {
                sageService.checkIfDocumentExistInSage(sourceDocument.documentValue!!, sageOrganizationResponse[0]!!, sourceDocument.orgSerialId, sourceDocument.accType, sageOrganizationResponse[1]!!)
            }
            val destinationPresentOnSage = sageService.checkIfDocumentExistInSage(destinationDocument.documentValue!!, sageOrganizationResponse[0]!!, destinationDocument.orgSerialId, destinationDocument.accType, sageOrganizationResponse[1]!!)

            val nullDoc: MutableList<String> = mutableListOf<String>()

            if (destinationPresentOnSage == null) {
                nullDoc.add(destinationDocument.documentValue!!)
            }

            if (sourcePresentOnSage == null) {
                nullDoc.add(sourceDocument.documentValue!!)
            }

            if (destinationPresentOnSage == null || sourcePresentOnSage == null) {
                recordAudits(settlementId, """$nullDoc""", "Documents must be posted on Sage", false)
                settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTING_FAILED, performedBy)
                return false
            }

            val matchingSettlementOnSageRequest: MutableList<SageSettlementRequest>? = mutableListOf()
            var flag = if (listOfRecOrPayCode.contains(sourceDocument.accType)) "P" else ""
            matchingSettlementOnSageRequest?.add(
                SageSettlementRequest(sourcePresentOnSage, sageOrganizationResponse[0]!!, settlement.amount?.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.DOWN).toString(), flag)
            )

            flag = if (listOfRecOrPayCode.contains(destinationDocument.accType)) "P" else ""
            matchingSettlementOnSageRequest?.add(
                SageSettlementRequest(destinationPresentOnSage, sageOrganizationResponse[0]!!, settlement.amount?.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.DOWN).toString(), flag)
            )

            val result = SageClient.postSettlementToSage(matchingSettlementOnSageRequest!!)
            val processedResponse = XML.toJSONObject(result.response)
            val status = getZstatus(processedResponse)
            if (status == "DONE") {
                settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTED, performedBy)
                recordAudits(settlementId, result.requestString, result.response, true)
                return true
            } else {
                settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTING_FAILED, performedBy)
                recordAudits(settlementId, result.requestString, result.response, false)
            }
        } catch (readTimeOutException: ReadTimeoutException) {
            logger().info("ReadTimeoutException: $settlementId")
            aresMessagePublisher.emitBulkMatchingSettlementOnSage(
                PostSettlementRequest(
                    settlementId = settlementId,
                    performedBy = performedBy
                )
            )
        } catch (sageException: SageException) {
            logger().info("SageException: $settlementId")
            settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTING_FAILED, performedBy)
            recordAudits(settlementId, sageException.data, sageException.context, false)
        } catch (aresException: AresException) {
            logger().info("AresException: $settlementId")
            settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTING_FAILED, performedBy)
            recordAudits(settlementId, settlementId.toString(), "${aresException.error.message} ${aresException.context}", false)
        } catch (e: Exception) {
            logger().info("Exception: $settlementId")
            settlementRepository.updateSettlementStatus(settlementId, SettlementStatus.POSTING_FAILED, performedBy)
            recordAudits(settlementId, settlementId.toString(), e.toString(), false)
        }
        return false
    }

    private suspend fun recordAudits(id: Long, request: String, response: String, isSuccess: Boolean) {
        thirdPartyApiAuditService.createAudit(
            ThirdPartyApiAudit(
                null,
                "MatchSettlementOnSage",
                "Settlement",
                id,
                "SETTLEMENT",
                if (isSuccess) "200" else "500",
                request,
                response,
                isSuccess
            )
        )
    }

    private suspend fun checkIfOrganizationIdIsValid(settlementId: Long, accMode: AccMode, accountUtilization: AccountUtilization): List<String?> {
        val sageOrganizationFromSageId: String?

        val registrationNumber: String?
        val serialId: Long?
        if (accountUtilization.organizationId != null) {
            val organization = railsClient.getListOrganizationTradePartyDetails(accountUtilization.organizationId!!)
            if (organization.list.isEmpty()) {
                throw AresException(AresError.ERR_1530, "")
            }
            registrationNumber = organization.list[0]["registration_number"].toString()
            serialId = organization.list[0]["serial_id"]!!.toString().toLong()
            val sageOrganizationQuery = if (accMode == AccMode.AR) "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='$registrationNumber'" else "Select BPSNUM_0 from $sageDatabase.BPSUPPLIER where XX1P4PANNO_0='$registrationNumber'"
            val resultFromSageOrganizationQuery = SageClient.sqlQuery(sageOrganizationQuery)
            val recordsForSageOrganization = ObjectMapper().readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)
            sageOrganizationFromSageId = if (accMode == AccMode.AR) recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId else recordsForSageOrganization.recordSet?.get(0)?.sageSupplierId
        } else {
            throw AresException(AresError.ERR_1532, "organizationId is not present")
        }

        val sageOrganizationResponse = cogoClient.getSageOrganization(
            SageOrganizationRequest(
                serialId.toString()
            )
        )

        if (sageOrganizationResponse.sageOrganizationId.isNullOrEmpty()) {
            recordAudits(settlementId, sageOrganizationResponse.toString(), "Sage organization not present", false)
            throw AresException(AresError.ERR_1532, "sage organizationId is not present in table")
        }

        if (sageOrganizationResponse.sageOrganizationId != sageOrganizationFromSageId) {
            recordAudits(settlementId, """Sage: $sageOrganizationFromSageId and Platform: ${sageOrganizationResponse.sageOrganizationId}""", "sage serial organization id different in sage db and cogoport db", false)
            throw AresException(AresError.ERR_1532, "sage serial organization id different in sage db and cogoport db")
        }

        return mutableListOf(sageOrganizationResponse.sageOrganizationId, registrationNumber)
    }

    @Transactional
    open suspend fun createTdsAsPaymentEntry(
        destId: Long,
        destType: SettlementType,
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        createdBy: UUID?,
        createdByUserType: String?,
        tdsType: SettlementType?,
        invoiceAndBillData: AccountUtilization?,
        exchangeRate: BigDecimal?,
        paymentTransactionDate: Date
    ): Long? {
        val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
        val accCodeAndSignFlag = when (invoiceAndBillData?.accMode) {
            AccMode.AR -> hashMapOf("signFlag" to -1, "accCode" to AresModelConstants.TDS_AR_ACCOUNT_CODE)
            else -> hashMapOf("signFlag" to 1, "accCode" to AresModelConstants.TDS_AP_ACCOUNT_CODE)
        }

        val paymentNum = when (invoiceAndBillData?.accMode) {
            AccMode.AR -> sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.CTDS.prefix)
            else -> sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.VTDS.prefix)
        }

        val paymentNumValue = "$tdsType${financialYearSuffix}$paymentNum"
        val serviceType = when (invoiceAndBillData?.serviceType.isNullOrEmpty()) {
            true -> ServiceType.NA
            else -> ServiceType.valueOf(invoiceAndBillData?.serviceType!!)
        }

        val paymentsRequest = Payment(
            id = null,
            entityType = invoiceAndBillData?.entityCode,
            sageOrganizationId = invoiceAndBillData?.sageOrganizationId,
            bankId = null,
            organizationId = invoiceAndBillData?.organizationId,
            paymentNum = paymentNum,
            paymentNumValue = paymentNumValue,
            refAccountNo = paymentNumValue,
            serviceType = serviceType,
            taggedOrganizationId = invoiceAndBillData?.taggedOrganizationId,
            tradePartyMappingId = invoiceAndBillData?.tradePartyMappingId,
            amount = tdsAmount,
            currency = currency,
            ledAmount = tdsLedAmount,
            ledCurrency = ledCurrency,
            accMode = invoiceAndBillData?.accMode,
            signFlag = accCodeAndSignFlag["signFlag"]?.toShort(),
            createdBy = createdBy.toString(),
            performedByUserType = createdByUserType,
            paymentDocumentStatus = PaymentDocumentStatus.APPROVED,
            accCode = accCodeAndSignFlag["accCode"],
            orgSerialId = invoiceAndBillData?.orgSerialId,
            organizationName = invoiceAndBillData?.organizationName,
            zone = invoiceAndBillData?.zoneCode,
            utr = "tds against $destType$destId",
            remarks = "tds against $destType$destId",
            updatedBy = createdBy.toString(),
            paymentCode = PaymentCode.valueOf(tdsType?.name!!),
            payMode = PayMode.BANK,
            docType = DocType.TDS,
            transactionDate = paymentTransactionDate,
            exchangeRate = exchangeRate
        )

        val payment = paymentConverter.convertToEntity(paymentsRequest)

        payment.migrated = false
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())

        val savedPayment = paymentRepository.save(payment)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PAYMENTS,
                objectId = savedPayment.id,
                actionName = AresConstants.CREATE,
                data = savedPayment,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )
        paymentsRequest.id = savedPayment.id

        val accUtilizationModel: AccUtilizationRequest = accUtilizationToPaymentConverter.convertEntityToModel(payment)

        accUtilizationModel.serviceType = serviceType
        accUtilizationModel.accType = AccountType.valueOf(tdsType.name)
        accUtilizationModel.zoneCode = invoiceAndBillData?.zoneCode
        accUtilizationModel.currencyPayment = tdsAmount
        accUtilizationModel.ledgerPayment = tdsLedAmount
        accUtilizationModel.ledgerAmount = tdsLedAmount
        accUtilizationModel.ledCurrency = ledCurrency
        accUtilizationModel.currency = currency!!
        accUtilizationModel.docStatus = DocumentStatus.FINAL
        accUtilizationModel.migrated = false
        accUtilizationModel.settlementEnabled = true

        val accUtilEntity = accUtilizationToPaymentConverter.convertModelToEntity(accUtilizationModel)

        accUtilEntity.documentNo = payment.paymentNum!!
        accUtilEntity.documentValue = payment.paymentNumValue
        accUtilEntity.taxableAmount = BigDecimal.ZERO
        accUtilEntity.accCode = accCodeAndSignFlag["accCode"]!!
        accUtilEntity.tdsAmount = BigDecimal.ZERO
        accUtilEntity.tdsAmountLoc = BigDecimal.ZERO
        accUtilEntity.isVoid = false

        val accUtilRes = accountUtilizationRepository.save(accUtilEntity)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilRes.id,
                actionName = AresConstants.CREATE,
                data = accUtilRes,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )

//        if (savedPayment.entityCode != 501 && (savedPayment.paymentCode in listOf(PaymentCode.REC, PaymentCode.CTDS))) {
// //            aresMessagePublisher.emitPostPaymentToSage(
// //                PostPaymentToSage(
// //                    paymentId = savedPayment.id!!,
// //                    performedBy = savedPayment.createdBy!!
// //                )
// //            )
//            try {
//                onAccountService.directFinalPostToSage(
//                    PostPaymentToSage(
//                        paymentId = savedPayment.id!!,
//                        performedBy = savedPayment.updatedBy!!
//                    )
//                )
//            } catch (ex: Exception) {
//                logger().info(ex.stackTraceToString())
//            }
//        }

        if (accUtilRes.accMode == AccMode.AP) {
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilRes.organizationId))
        }
        if (accUtilRes.accMode == AccMode.AR) {
            aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilRes.organizationId))
        }

        return savedPayment.paymentNum!!
    }

    override suspend fun sendEmailSettlementsMatchingFailed(url: String) {
        val emailVariables = HashMap<String, String?>()
        emailVariables["sheet_url"] = url

        val request = CreateCommunicationRequest(
            templateName = AresConstants.FAILED_SETTLEMENTS_MATCHING_ON_SAGE_TEMPLATE,
            performedByUserId = AresConstants.ARES_USER_ID,
            performedByUserName = "Ares",
            recipientEmail = AresConstants.RECIPIENT_EMAIL_FOR_EVERYDAY_AUTO_GENERATION_SETTLEMENTS_MATCHING_FAILED_EMAIL,
            senderEmail = AresConstants.NO_REPLY,
            ccEmails = AresConstants.CC_MAIL_FOR_SETTLEMENTS_MATCHING_FAILED_ON_SAGE,
            emailVariables = emailVariables
        )

        aresMessagePublisher.sendEmail(request)
    }

    override suspend fun getSettlementList(request: SettlementHistoryRequest): ResponseList<SettlementListDoc?> {
        val possibleAccTypes = stringAccountTypes(request)
        val query = util.toQueryString(request.query)

        val entityCodes = when (request.entityCode != null) {
            true -> when (request.entityCode) {
                AresConstants.ENTITY_101 -> listOf(AresConstants.ENTITY_101, AresConstants.ENTITY_201, AresConstants.ENTITY_301, AresConstants.ENTITY_401)
                else -> listOf(request.entityCode)
            }
            else -> null
        }

        val settlementDocs = settlementRepository.getSettlementList(
            request.orgId!!,
            possibleAccTypes,
            request.page,
            request.pageLimit,
            query,
            entityCodes,
            request.sortBy,
            request.sortType
        )

        if (settlementDocs.isNullOrEmpty()) return ResponseList()

        val validAccTypeForIRN = listOf(AccountType.SINV, AccountType.SCN)

        val invoiceNumberList = mutableListOf<String>()
        val invoiceIdToAccTypeMap = settlementDocs.associateBy({ it.sourceId }, { it.sourceAccType }) +
            settlementDocs.associateBy({ it.destinationId }, { it.destinationAccType })

        invoiceIdToAccTypeMap.forEach { (invoiceId, accType) ->
            if (accType in validAccTypeForIRN) {
                invoiceNumberList.add(Hashids.encode(invoiceId))
            }
        }

        val updatedSettlementDocs = getInvoiceAdditionalData(invoiceNumberList, mutableListOf("IrnNumber"), settlementDocs)

        val totalRecords = settlementRepository.getSettlementCount(
            request.orgId!!,
            possibleAccTypes,
            query,
            entityCodes
        )

        return ResponseList(
            list = updatedSettlementDocs,
            totalPages = Utilities.getTotalPages(totalRecords, request.pageLimit),
            totalRecords,
            pageNo = request.page
        )
    }

    private suspend fun getInvoiceAdditionalData(invoiceIds: MutableList<String>, keys: MutableList<String>, settlementDocs: List<SettlementListDoc>): List<SettlementListDoc> {
        val invoiceAdditionalData = plutusClient.getInvoiceAdditionalList(invoiceIds, keys)

        settlementDocs.forEach { doc ->
            invoiceAdditionalData?.let { data ->
                val sourceInvoiceAdditionalDoc = data.firstOrNull { it.invoiceId == doc.sourceId && it.key == "IrnNumber" }
                val destinationInvoiceAdditionalDoc = data.firstOrNull { it.invoiceId == doc.destinationId && it.key == "IrnNumber" }

                sourceInvoiceAdditionalDoc?.let {
                    if (it.value.toString().isNotBlank()) {
                        doc.sourceIrnNumber = it.value.toString()
                    }
                }

                destinationInvoiceAdditionalDoc?.let {
                    if (it.value.toString().isNotBlank()) {
                        doc.destinationIrnNumber = it.value.toString()
                    }
                }
            }
        }

        return settlementDocs
    }

    private suspend fun createTdsAsJv(
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        createdBy: UUID?,
        createdByUserType: String?,
        invoiceAndBillData: AccountUtilization?,
        exchangeRate: BigDecimal?,
        paymentTransactionDate: Date,
        utr: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?
    ): Long? {
        val lineItemProps: MutableList<HashMap<String, Any?>> = mutableListOf(
            hashMapOf(
                "accMode" to "AP",
                "glCode" to "321000",
                "type" to "DEBIT",
                "signFlag" to 1
            ),
            hashMapOf(
                "accMode" to "VTDS",
                "glCode" to "324003",
                "type" to "CREDIT",
                "signFlag" to -1
            )
        )
        return parentJvService.createTdsAsJvForBills(
            currency,
            ledCurrency,
            tdsAmount,
            tdsLedAmount,
            createdBy,
            createdByUserType,
            invoiceAndBillData,
            exchangeRate,
            paymentTransactionDate,
            lineItemProps,
            utr,
            payCurrTds,
            payLocTds
        )
    }
}
