package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.mapper.CollectionTrendMapper
import com.cogoport.ares.api.payment.mapper.DailyOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingMapper
import com.cogoport.ares.api.payment.mapper.OverallStatsMapper
import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.CollectionTrendResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Month
import java.time.format.DateTimeFormatter

@Singleton
class OpenSearchServiceImpl : OpenSearchService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var dsoConverter: DailyOutstandingMapper

    @Inject
    lateinit var dpoConverter: DailyOutstandingMapper

    @Inject
    lateinit var collectionTrendConverter: CollectionTrendMapper

    @Inject
    lateinit var overallStatsConverter: OverallStatsMapper

    @Inject
    lateinit var outstandingConverter: OutstandingMapper

    @Inject
    lateinit var orgOutstandingConverter: OrgOutstandingMapper

    /**
     * @param: OpenSearchRequest
     */
    override suspend fun pushDashboardData(request: OpenSearchRequest) {
        when (request.accMode) {
            AccMode.AR -> { updateReceivables(request) }
            AccMode.AP -> { updatePayable(request) }
            else -> {
                updateReceivables(request)
                updatePayable(request)
            }
        }
    }

    private suspend fun updateReceivables(request: OpenSearchRequest) {
        val zone = request.zone!!
        val quarter = request.quarter
        val date = request.date
        val year = request.year
        val serviceType = request.serviceType!!
        val invoiceCurrency = request.invoiceCurrency!!
        /** Collection Trend */
        logger().info("Updating Collection Trend document")
        generateCollectionTrend(zone, quarter, year, serviceType,invoiceCurrency)

        /** Overall Stats */
        logger().info("Updating Overall Stats document")
        generateOverallStats(zone, quarter, year, serviceType,invoiceCurrency)

        /** Monthly Outstanding */
        logger().info("Updating Monthly Outstanding document")
        generateMonthlyOutstanding(zone, quarter, year, serviceType,invoiceCurrency)

        /** Quarterly Outstanding */
        logger().info("Updating Quarterly Outstanding document")
        generateQuarterlyOutstanding(zone, quarter, year, serviceType,invoiceCurrency)


        /** Daily Sales Outstanding */
        generateDailySalesOutstanding(zone, quarter, year, serviceType,invoiceCurrency,date)
    }

    override suspend fun generateCollectionTrend(zone: String,quarter: Int, year: Int,serviceType: ServiceType, invoiceCurrency: String ){
        val collectionZoneResponse = accountUtilizationRepository.generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency)
        updateCollectionTrend(zone, quarter, year, collectionZoneResponse, serviceType, invoiceCurrency)
        val collectionResponseAll = accountUtilizationRepository.generateCollectionTrend(null, quarter, year, null, null)
        updateCollectionTrend(null, quarter, year, collectionResponseAll, null, null)
    }

    override suspend fun generateOverallStats (zone: String,quarter: Int, year: Int,serviceType: ServiceType, invoiceCurrency: String){
        val statsZoneData = accountUtilizationRepository.generateOverallStats(zone, serviceType, invoiceCurrency)
        updateOverallStats(zone, statsZoneData, serviceType, invoiceCurrency)
        val statsAllData = accountUtilizationRepository.generateOverallStats(null, null, null)
        updateOverallStats(null, statsAllData, null, null)
    }

    override suspend fun generateMonthlyOutstanding (zone: String,quarter: Int, year: Int,serviceType: ServiceType, invoiceCurrency: String){
        val monthlyTrendZoneData = accountUtilizationRepository.generateMonthlyOutstanding(zone, serviceType, invoiceCurrency)
        updateMonthlyTrend(zone, monthlyTrendZoneData, serviceType, invoiceCurrency)
        val monthlyTrendAllData = accountUtilizationRepository.generateMonthlyOutstanding(null, null, null)
        updateMonthlyTrend(null, monthlyTrendAllData, null, null)
    }


    override suspend fun generateQuarterlyOutstanding (zone: String?,quarter: Int?, year: Int?,serviceType: ServiceType?, invoiceCurrency: String?){
        val quarterlyTrendZoneData = accountUtilizationRepository.generateQuarterlyOutstanding(zone, serviceType, invoiceCurrency)
        updateQuarterlyTrend(zone, quarterlyTrendZoneData, serviceType, invoiceCurrency)
        val quarterlyTrendAllData = accountUtilizationRepository.generateQuarterlyOutstanding(null, null, null)
        updateQuarterlyTrend(null, quarterlyTrendAllData, null, null)
    }

    override suspend fun generateDailySalesOutstanding (zone: String,quarter: Int, year: Int,serviceType: ServiceType, invoiceCurrency: String, date:String){
        logger().info("Updating Daily Sales Outstanding document")
        val dailySalesZoneServiceTypeData = accountUtilizationRepository.generateDailySalesOutstanding(zone, date, serviceType, invoiceCurrency)
        updateDailySalesOutstanding(zone, year, dailySalesZoneServiceTypeData, serviceType, invoiceCurrency)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, date, null, null)
        updateDailySalesOutstanding(null, year, dailySalesAllData, null, null)
    }
    /**
     * This updates the data for Daily Payables Outstanding graph on OpenSearch on receipt of new Bill
     * @param : OpenSearchRequest
     */
    private suspend fun updatePayable(request: OpenSearchRequest) {
        /** Daily Payable Outstanding */
        logger().info("Updating Payable Outstanding document")
        val dailyPayableZoneData = accountUtilizationRepository.generateDailyPayablesOutstanding(request.zone, request.date)
        updateDailyPayablesOutstanding(request.zone, request.year, dailyPayableZoneData)
        val dailyPayableAllData = accountUtilizationRepository.generateDailyPayablesOutstanding(null, request.date)
        updateDailyPayablesOutstanding(null, request.year, dailyPayableAllData)
    }

    private fun updateCollectionTrend(zone: String?, quarter: Int, year: Int, data: MutableList<CollectionTrend>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        if (data.isNullOrEmpty()) return

        val collectionData = data.map {
            collectionTrendConverter.convertToModel(it)
        }

        var zoneKey: String? = null
        var serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null

        if (zone.isNullOrBlank()) zoneKey = "ALL" else zoneKey = zone?.uppercase()

        if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else serviceTypeKey = serviceType?.name

        if (invoiceCurrency.isNullOrBlank()) invoiceCurrencyKey = "ALL" else invoiceCurrencyKey = invoiceCurrency?.uppercase()

        val collectionId = AresConstants.COLLECTIONS_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + invoiceCurrencyKey + AresConstants.KEY_DELIMITER + year + AresConstants.KEY_DELIMITER + "Q$quarter"
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionId, formatCollectionTrend(collectionData, collectionId, quarter, serviceType, invoiceCurrency))
    }

    private fun updateOverallStats(zone: String?, data: OverallStats, serviceType: ServiceType?, invoiceCurrency: String?) {
        val overallStatsData = overallStatsConverter.convertToModel(data)

        var zoneKey: String? = null
        var serviceTypeKey: String? = null

        var invoiceCurrencyKey: String? = null

        if (zone.isNullOrBlank()) zoneKey = "ALL" else zoneKey = zone?.uppercase()
        if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else serviceTypeKey = serviceType?.name
        if (invoiceCurrency.isNullOrBlank()) invoiceCurrencyKey = "ALL" else invoiceCurrencyKey = invoiceCurrency

        val statsId = AresConstants.OVERALL_STATS_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + invoiceCurrencyKey

        if (overallStatsData != null) {
            overallStatsData?.id = statsId
            OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, overallStatsData)
        }
    }

    private fun updateMonthlyTrend(zone: String?, data: MutableList<Outstanding>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        if (data.isNullOrEmpty()) return
        val monthlyTrend = data.map { outstandingConverter.convertToModel(it) }

        var zoneKey: String? = null
        var serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null

        if (zone.isNullOrBlank()) zoneKey = "ALL" else zoneKey = zone?.uppercase()

        if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else serviceTypeKey = serviceType?.name

        if (invoiceCurrency.isNullOrBlank()) invoiceCurrencyKey = "ALL" else invoiceCurrencyKey = invoiceCurrency?.uppercase()

        val monthlyTrendId = AresConstants.MONTHLY_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + invoiceCurrencyKey

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyTrendId, MonthlyOutstanding(monthlyTrend, monthlyTrendId))
    }

    private fun updateQuarterlyTrend(zone: String?, data: MutableList<Outstanding>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        if (data.isNullOrEmpty()) return

        var zoneKey: String? = null
        var serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null

        if (zone.isNullOrBlank()) zoneKey = "ALL" else zoneKey = zone?.uppercase()

        if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else serviceTypeKey = serviceType?.name!!

        if (invoiceCurrency.isNullOrBlank()) invoiceCurrencyKey = "ALL" else invoiceCurrencyKey = invoiceCurrency?.uppercase()
        val quarterlyTrendId = AresConstants.QUARTERLY_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + invoiceCurrencyKey
        val quarterlyTrend = data.map { outstandingConverter.convertToModel(it) }
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId, QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId))
    }

    private fun updateDailySalesOutstanding(zone: String?, year: Int, data: DailyOutstanding, serviceType: ServiceType?, invoiceCurrency: String?) {
        val dsoResponse = dsoConverter.convertToModel(data)
        var zoneKey = ""
        var serviceTypeKey = ""
        var invoiceCurrencyKey = ""

        if (zone.isNullOrBlank()) zoneKey = "ALL" else zoneKey = zone?.uppercase()

        if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else serviceTypeKey = serviceType?.name!!

        if (invoiceCurrency.isNullOrBlank()) invoiceCurrencyKey = "ALL" else invoiceCurrencyKey = invoiceCurrency?.uppercase()

        if (dsoResponse != null) {
            val dailySalesId = AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + invoiceCurrencyKey + AresConstants.KEY_DELIMITER + dsoResponse?.month + AresConstants.KEY_DELIMITER + year
            OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dsoResponse)
        }
    }

    private fun updateDailyPayablesOutstanding(zone: String?, year: Int, data: DailyOutstanding) {
        val dpoResponse = dpoConverter.convertToModel(data)
        if (data != null) {
            val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + dpoResponse.month + AresConstants.KEY_DELIMITER + year else AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + zone + AresConstants.KEY_DELIMITER + dpoResponse.month + AresConstants.KEY_DELIMITER + year
            OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dpoResponse)
        }
    }

    private fun formatCollectionTrend(data: List<CollectionTrendResponse>, id: String, quarter: Int, serviceType: ServiceType?, invoiceCurrency: String?): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
        var dashboardCurrency: String? = null
//        var serviceType: ServiceType? = null

        val monthList = mutableListOf<String?>()
        for (row in data) {
            if (row.duration != "Total") {
                trendData.add(CollectionTrendResponse(row.duration, row.receivableAmount, row.collectableAmount, row.dashboardCurrency))
                monthList.add(row.duration)
            } else {
                totalAmount = row.receivableAmount
                totalCollected = row.collectableAmount
                dashboardCurrency = row.dashboardCurrency
            }
        }
        getMonthFromQuarter(quarter).forEach {
            if (!monthList.contains(it)) {
                trendData.add(CollectionTrendResponse(it, 0.toBigDecimal(), 0.toBigDecimal(), null))
            }
        }
        return CollectionResponse(totalAmount, totalCollected, trendData.sortedBy { Month.valueOf(it.duration!!.uppercase()) }, id, dashboardCurrency)
    }

    private fun getMonthFromQuarter(quarter: Int): List<String> {
        return when (quarter) {
            1 -> { listOf("January", "February", "March") }
            2 -> { listOf("April", "May", "June") }
            3 -> { listOf("July", "August", "September") }
            4 -> { listOf("October", "November", "December") }
            else -> { throw AresException(AresError.ERR_1004, "") }
        }
    }

    /** Outstanding Data */
    override suspend fun pushOutstandingData(request: OpenSearchRequest) {
        if (request.orgId.isEmpty()) {
            throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, null).also {
            updateOrgOutstanding(null, request.orgName, request.orgId, it)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, request.zone).also {
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

    private fun validateDueAmount(data: MutableList<DueAmount>) {
        data.forEach { if (it.amount == 0.toBigDecimal()) it.amount = 0.0.toBigDecimal() }
        listOf("INR", "USD").forEach { curr ->
            if (curr !in data.groupBy { it.currency }) {
                data.add(DueAmount(curr, 0.0.toBigDecimal(), 0))
            }
        }
    }
}
