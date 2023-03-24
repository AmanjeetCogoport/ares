package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.ServiceLevelOutstanding
import com.cogoport.ares.api.common.models.TradeAndServiceLevelOutstanding
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.UUID

@Singleton
class OpenSearchServiceImpl : OpenSearchService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var orgOutstandingConverter: OrgOutstandingMapper

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    /** Outstanding Data */
    override suspend fun pushOutstandingData(request: OpenSearchRequest) {
        if (request.orgId.isEmpty()) {
            throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, null, null).also {
            updateOrgOutstanding(null, request.orgName, request.orgId, it)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, request.zone, null).also {
            updateOrgOutstanding(request.zone, request.orgName, request.orgId, it)
        }
    }

    /**
     * Push List of Organization outstanding data to open search.
     * @param: openSearchListRequest
     */
    override suspend fun pushOutstandingListData(openSearchListRequest: OpenSearchListRequest) {
        for (organization in openSearchListRequest.openSearchList) {
            pushOutstandingData(
                OpenSearchRequest(
                    zone = organization.zone,
                    date = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern(AresConstants.YEAR_DATE_FORMAT)),
                    quarter = AresConstants.CURR_QUARTER,
                    year = AresConstants.CURR_YEAR,
                    orgId = organization.orgId,
                    orgName = organization.orgName,
                    accMode = null
                )
            )
        }
    }

    private fun updateOrgOutstanding(zone: String?, orgName: String?, orgId: String?, data: List<OrgOutstanding>) {
        if (data.isEmpty()) return
        val dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
        val invoicesDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.openInvoicesAmount.toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
        val paymentsDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.paymentsAmount.toString().toBigDecimal() }, it.value.sumOf { it.paymentsCount!! }) }.toMutableList()
        val outstandingDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.outstandingAmount.toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
        val invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
        val paymentsCount = dataModel.sumOf { it.paymentsCount!! }
        val invoicesLedAmount = dataModel.sumOf { it.openInvoicesLedAmount!! }
        val paymentsLedAmount = dataModel.sumOf { it.paymentsLedAmount!! }
        val outstandingLedAmount = dataModel.sumOf { it.outstandingLedAmount!! }
        validateDueAmount(invoicesDues)
        validateDueAmount(paymentsDues)
        validateDueAmount(outstandingDues)
        val orgOutstanding = CustomerOutstanding(orgId, orgName, zone, InvoiceStats(invoicesCount, invoicesLedAmount, invoicesDues.sortedBy { it.currency }), InvoiceStats(paymentsCount, paymentsLedAmount, paymentsDues.sortedBy { it.currency }), InvoiceStats(invoicesCount, outstandingLedAmount, outstandingDues.sortedBy { it.currency }), null)
        val docId = if (zone != null) "${orgId}_$zone" else "${orgId}_ALL"
        OpenSearchClient().updateDocument(AresConstants.SALES_OUTSTANDING_INDEX, docId, orgOutstanding)
    }

    fun validateDueAmount(data: MutableList<DueAmount>) {
        data.forEach { if (it.amount == 0.toBigDecimal()) it.amount = 0.0.toBigDecimal() }
        listOf("INR", "USD").forEach { curr ->
            if (curr !in data.groupBy { it.currency }) {
                data.add(DueAmount(curr, 0.0.toBigDecimal(), 0))
            }
        }
    }

    private fun generatingOpenSearchKey(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?): Map<String, String?> {
        var zoneKey: String? = null
        val serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null
        zoneKey = if (zone.isNullOrBlank()) "ALL" else zone.uppercase()

        invoiceCurrencyKey = if (invoiceCurrency.isNullOrBlank()) "ALL" else invoiceCurrency.uppercase()
        return mapOf("zoneKey" to zoneKey, "serviceTypeKey" to serviceTypeKey, "invoiceCurrencyKey" to invoiceCurrencyKey)
    }

    override suspend fun generateOutstandingData(searchKey: String, entityCode: Int?, defaultersOrgIds: List<UUID>?, dashboardCurrency: String?) {
        val data = unifiedDBRepo.getOutstandingData(entityCode, defaultersOrgIds)
        val mapData = hashMapOf<String, ServiceLevelOutstanding> ()

        if (!data.isNullOrEmpty()) {
            val onAccountAmount = unifiedDBRepo.getOnAccountAmount(entityCode, defaultersOrgIds)
            val onAccountAmountForPastSevenDays = unifiedDBRepo.getOnAccountAmountForPastSevenDays(entityCode, defaultersOrgIds)
            val openInvoiceAmountForPastSevenDays = unifiedDBRepo.getOutstandingAmountForPastSevenDays(entityCode, defaultersOrgIds)

            data.map { it.tradeType = it.tradeType?.uppercase() }
            data.map { it.serviceType = it.serviceType?.uppercase() }

            data.groupBy { it.groupedServices }.filter { it.key != null }.entries.map { (k, v) ->
                mapData[k.toString()] = ServiceLevelOutstanding(
                    openInvoiceAmount = v.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP),
                    currency = dashboardCurrency,
                    tradeType = getTradeAndServiceWiseData(v)
                )
            }

            val onAccountAmountForPastSevenDaysPercentage = when (onAccountAmount != BigDecimal.ZERO) {
                true -> onAccountAmountForPastSevenDays?.div(onAccountAmount?.setScale(4, RoundingMode.UP)!!)
                    ?.times(100.toBigDecimal())?.toLong()
                else -> BigDecimal.ZERO
            }

            val outstandingOpenSearchResponse = OutstandingOpensearchResponse(
                overallStats = OverallStats(
                    totalOutstandingAmount = data.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP),
                    openInvoicesAmount = data.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP),
                    customersCount = data.sumOf { it.customersCount!! },
                    dashboardCurrency = data.first().currency!!,
                    openInvoicesCount = data.sumOf { it.openInvoicesCount!! },
                    openInvoiceAmountForPastSevenDaysPercentage = openInvoiceAmountForPastSevenDays?.div(data.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP))?.times(100.toBigDecimal())?.toLong(),
                    onAccountAmount = onAccountAmount?.setScale(4, RoundingMode.UP),
                    onAccountAmountForPastSevenDaysPercentage = onAccountAmountForPastSevenDaysPercentage?.toLong()
                ),
                outstandingServiceWise = mapData
            )

            OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, searchKey, outstandingOpenSearchResponse)
        }
    }

    private fun getTradeAndServiceWiseData(value: List<OutstandingDocument>): List<TradeAndServiceLevelOutstanding> {
        val updatedList = mutableListOf<TradeAndServiceLevelOutstanding>()
        value.map { item ->
            if (item.serviceType == null || item.tradeType == null) {
                val document = updatedList.filter { it.key == "others" }
                if (document.isNotEmpty()) {
                    document.first().openInvoiceAmount = document.first().openInvoiceAmount.plus(item.openInvoiceAmount).setScale(4, RoundingMode.UP)
                } else {
                    val tradeAndServiceWiseDocument = TradeAndServiceLevelOutstanding(
                        key = "others",
                        name = "others",
                        openInvoiceAmount = item.openInvoiceAmount.setScale(4, RoundingMode.UP),
                        currency = item.currency
                    )
                    updatedList.add(tradeAndServiceWiseDocument)
                }
            } else {
                val document = updatedList.filter { it.key == "${item.serviceType}_${item.tradeType}" }
                if (document.isNotEmpty()) {
                    document.first().openInvoiceAmount = document.first().openInvoiceAmount.plus(item.openInvoiceAmount).setScale(4, RoundingMode.UP)
                } else {
                    val tradeAndServiceWiseDocument = TradeAndServiceLevelOutstanding(
                        key = "${item.serviceType}_${item.tradeType}",
                        name = "${item.serviceType} ${item.tradeType}",
                        openInvoiceAmount = item.openInvoiceAmount.setScale(4, RoundingMode.UP),
                        currency = item.currency
                    )
                    updatedList.add(tradeAndServiceWiseDocument)
                }
            }
        }
        return updatedList
    }

    override suspend fun paymentDocumentStatusMigration() {
        val pdsRecords = paymentRepository.getPaymentDocumentStatusWiseIds()
        pdsRecords.forEach {
            it.paymentIds.chunked(5000)
                .forEach { batch ->
                    bulkUpdate(it.paymentDocumentStatus, batch)
                }
        }
    }

    private fun bulkUpdate(paymentDocumentStatus: PaymentDocumentStatus, ids: List<Long>) {
        Client.updateByQuery { s ->
            s.index(AresConstants.ON_ACCOUNT_PAYMENT_INDEX).script { s ->
                s.inline { i -> i.source("ctx._source[\"paymentDocumentStatus\"] = \"${paymentDocumentStatus.name}\"").lang("painless") }
            }.query { q ->
                q.bool { b ->
                    b.must { s ->
                        s.terms { v ->
                            v.field("id").terms(
                                TermsQueryField.of { a ->
                                    a.value(
                                        ids.map {
                                            FieldValue.of(it)
                                        }
                                    )
                                }
                            )
                        }
                    }
                    b
                }
            }.refresh(true)
            s
        }
    }
}
