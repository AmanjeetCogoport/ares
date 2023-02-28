package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceTimeLineResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.common.models.ServiceLevelOutstanding
import com.cogoport.ares.api.common.models.TradeAndServiceLevelOutstanding
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
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.CollectionTrendResponse
import com.cogoport.ares.model.payment.response.DailyOutstandingResponse
import com.cogoport.ares.model.payment.response.DailyOutstandingResponseData
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsResponseData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

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

    @Inject
    lateinit var businessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

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
        val zone = request.zone
        val quarter = request.quarter
        val date = request.date
        val year = request.year
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val dashboardCurrency = when (invoiceCurrency.isNullOrEmpty()) {
            true -> "INR"
            false -> request.invoiceCurrency
        }
        val defaultersOrgIds = businessPartnersServiceImpl.listTradePartyDetailIds()

        /** Collection Trend */
        logger().info("Updating Collection Trend document")
        generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

        /** Overall Stats */
        logger().info("Updating Overall Stats document")
        generateOverallStats(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

        /** Monthly Outstanding */
        logger().info("Updating Monthly Outstanding document")
        generateMonthlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

        /** Quarterly Outstanding */
        logger().info("Updating Quarterly Outstanding document")
        generateQuarterlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

        /** Daily Sales Outstanding */
        logger().info("Updating Daily Outstanding Outstanding document")
        generateDailySalesOutstanding(zone, quarter, year, serviceType, invoiceCurrency, date, dashboardCurrency, defaultersOrgIds)
    }

    override suspend fun generateCollectionTrend(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?) {
        val collectionZoneResponse = accountUtilizationRepository.generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)
        updateCollectionTrend(zone, quarter, year, collectionZoneResponse, serviceType, invoiceCurrency)
        val collectionResponseAll = accountUtilizationRepository.generateCollectionTrend(null, quarter, year, null, null, defaultersOrgIds)
        updateCollectionTrend(null, quarter, year, collectionResponseAll, null, null)
    }

    override suspend fun generateOverallStats(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?) {
        val statsZoneData = accountUtilizationRepository.generateOverallStats(zone, serviceType, invoiceCurrency, defaultersOrgIds)
        updateOverallStats(zone, statsZoneData, serviceType, invoiceCurrency)
        val statsAllData = accountUtilizationRepository.generateOverallStats(null, null, null, defaultersOrgIds)
        updateOverallStats(null, statsAllData, null, null)
    }

    override suspend fun generateMonthlyOutstanding(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?) {
        val monthlyTrendZoneData = accountUtilizationRepository.generateMonthlyOutstanding(zone, serviceType, invoiceCurrency, defaultersOrgIds)

        monthlyTrendZoneData?.forEach { it ->
            if (it.dashboardCurrency.isNullOrEmpty()) {
                it.dashboardCurrency = invoiceCurrency!!
            }
        }
        updateMonthlyTrend(zone, monthlyTrendZoneData, serviceType, invoiceCurrency)
        val monthlyTrendAllData = accountUtilizationRepository.generateMonthlyOutstanding(null, null, null, defaultersOrgIds)
        updateMonthlyTrend(null, monthlyTrendAllData, null, null)
    }

    override suspend fun generateQuarterlyOutstanding(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?) {
        val quarterlyTrendZoneData = accountUtilizationRepository.generateQuarterlyOutstanding(zone, serviceType, invoiceCurrency, defaultersOrgIds)
        quarterlyTrendZoneData?.forEach { it ->
            if (it.dashboardCurrency.isNullOrEmpty()) {
                it.dashboardCurrency = invoiceCurrency!!
            }
        }
        updateQuarterlyTrend(zone, quarterlyTrendZoneData, serviceType, invoiceCurrency)
        val quarterlyTrendAllData = accountUtilizationRepository.generateQuarterlyOutstanding(null, null, null, defaultersOrgIds)
        updateQuarterlyTrend(null, quarterlyTrendAllData, null, null)
    }

    override suspend fun generateDailySalesOutstanding(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, date: String, dashboardCurrency: String, defaultersOrgIds: List<UUID>?) {
        logger().info("Updating Daily Sales Outstanding document")
        val dailySalesZoneServiceTypeData = accountUtilizationRepository.generateDailySalesOutstanding(zone, date, serviceType, invoiceCurrency, defaultersOrgIds)
        dailySalesZoneServiceTypeData.map {
            if (it.dashboardCurrency == null) {
                it.dashboardCurrency = invoiceCurrency
            }
        }

        updateDailySalesOutstanding(zone, year, dailySalesZoneServiceTypeData, serviceType, invoiceCurrency, date, dashboardCurrency)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, date, null, null, defaultersOrgIds)
        updateDailySalesOutstanding(null, year, dailySalesAllData, null, null, date, dashboardCurrency)
    }
    /**
     * This updates the data for Daily Payables Outstanding graph on OpenSearch on receipt of new Bill
     * @param : OpenSearchRequest
     */
    private suspend fun updatePayable(request: OpenSearchRequest) {
        /** Daily Payable Outstanding */
        logger().info("Updating Payable Outstanding document")
        val dashboardCurrency = when (request.invoiceCurrency.isNullOrEmpty()) {
            true -> "INR"
            false -> request.invoiceCurrency
        }
        generateDailyPayableOutstanding(request.zone, request.quarter, request.year, request.serviceType, request.invoiceCurrency, request.date, dashboardCurrency)
    }

    override suspend fun generateDailyPayableOutstanding(
        zone: String?,
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        invoiceCurrency: String?,
        date: String,
        dashboardCurrency: String
    ) {
        val dailyPayableZoneServiceTypeData = accountUtilizationRepository.generateDailyPayableOutstanding(zone, date, serviceType, invoiceCurrency)
        updateDailyPayableOutstanding(zone, year, dailyPayableZoneServiceTypeData, serviceType, invoiceCurrency, date, dashboardCurrency)
        val dailyPayableAllData = accountUtilizationRepository.generateDailyPayableOutstanding(null, date, null, null)
        updateDailyPayableOutstanding(null, year, dailyPayableAllData, null, null, date, dashboardCurrency)
    }

    private fun updateCollectionTrend(zone: String?, quarter: Int, year: Int, data: MutableList<CollectionTrend>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)

        val collectionId = AresConstants.COLLECTIONS_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + year + AresConstants.KEY_DELIMITER + "Q$quarter"

        val collectionData = when (data.isNullOrEmpty()) {
            true -> listOf(
                CollectionTrendResponse(
                    duration = "Total",
                    receivableAmount = 0.toBigDecimal(),
                    collectableAmount = 0.toBigDecimal(),
                    dashboardCurrency = "INR"
                )
            )

            false -> data.map {
                collectionTrendConverter.convertToModel(it)
            }
        }
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionId, formatCollectionTrend(collectionData, collectionId, quarter, serviceType, invoiceCurrency))
    }

    private fun updateOverallStats(zone: String?, data: MutableList<OverallStats>, serviceType: ServiceType?, invoiceCurrency: String?) {
        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
        val statsId = AresConstants.OVERALL_STATS_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]

        val overallStatsData = when (data.isEmpty()) {
            true -> listOf(
                OverallStatsResponseData(
                    dashboardCurrency = "INR"
                )
            )
            false -> data.map {
                overallStatsConverter.convertToModel(it)
            }
        }

        val formattedData = OverallStatsResponse(
            id = statsId,
            list = overallStatsData
        )
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, formattedData)
    }

    private fun updateMonthlyTrend(zone: String?, data: MutableList<Outstanding>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        if (data.isNullOrEmpty()) return
        val monthlyTrend = data.map { outstandingConverter.convertToModel(it) }
        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
        val monthlyTrendId = AresConstants.MONTHLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyTrendId, MonthlyOutstanding(monthlyTrend, monthlyTrendId))
    }

    private fun updateQuarterlyTrend(zone: String?, data: MutableList<Outstanding>?, serviceType: ServiceType?, invoiceCurrency: String?) {
        if (data.isNullOrEmpty()) return
        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
        val quarterlyTrendId = AresConstants.QUARTERLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]
        val quarterlyTrend = data.map { outstandingConverter.convertToModel(it) }
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId, QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId))
    }

    private fun updateDailySalesOutstanding(zone: String?, year: Int, data: List<DailyOutstanding>, serviceType: ServiceType?, invoiceCurrency: String?, date: String?, dashboardCurrency: String) {
        val month = date?.substring(5, 7)
        val dsoResponse = when (data.isEmpty()) {
            true -> listOf(
                DailyOutstandingResponseData(
                    month = month?.toInt()!!,
                    days = YearMonth.of(year, month.toInt()).lengthOfMonth(),
                    openInvoiceAmount = 0.toBigDecimal(),
                    onAccountPayment = 0.toBigDecimal(),
                    outstandings = 0.toBigDecimal(),
                    totalSales = 0.toBigDecimal(),
                    value = 0.toBigDecimal(),
                    dashboardCurrency = dashboardCurrency
                )
            )
            false -> data.map {
                dsoConverter.convertToModel(it)
            }
        }

        dsoResponse.forEach {
            it.onAccountPayment = it.onAccountPayment?.times(1.0.toBigDecimal())
            it.totalSales = it.totalSales?.times(1.0.toBigDecimal())
        }

        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
        val dailySalesId = AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + month + AresConstants.KEY_DELIMITER + year

        val formattedData = DailyOutstandingResponse(
            list = dsoResponse,
            id = dailySalesId
        )

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, formattedData)
    }

    private fun updateDailyPayableOutstanding(zone: String?, year: Int, data: List<DailyOutstanding>, serviceType: ServiceType?, invoiceCurrency: String?, date: String, dashboardCurrency: String) {
        val month = date.substring(5, 7)
        val dpoResponse = when (data.isEmpty()) {
            true -> listOf(
                DailyOutstandingResponseData(
                    month = month.toInt(),
                    days = YearMonth.of(year, date.substring(5, 7).toInt()).lengthOfMonth(),
                    openInvoiceAmount = 0.toBigDecimal(),
                    onAccountPayment = 0.toBigDecimal(),
                    outstandings = 0.toBigDecimal(),
                    totalSales = 0.toBigDecimal(),
                    value = 0.toBigDecimal(),
                    dashboardCurrency = dashboardCurrency
                )
            )
            false -> data.map {
                dpoConverter.convertToModel(it)
            }
        }

        dpoResponse.forEach {
            it.onAccountPayment = it.onAccountPayment?.times(1.0.toBigDecimal())
            it.totalSales = it.totalSales?.times(1.0.toBigDecimal())
        }

        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
        val dailyPayablesId = AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + month + AresConstants.KEY_DELIMITER + year

        val formattedData = DailyOutstandingResponse(
            list = dpoResponse,
            id = dailyPayablesId
        )

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailyPayablesId, formattedData)
    }

    private fun formatCollectionTrend(data: List<CollectionTrendResponse>, id: String, quarter: Int, serviceType: ServiceType?, invoiceCurrency: String?): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
        var dashboardCurrency: String? = null

        val monthList = mutableListOf<String?>()
        for (row in data) {
            if (row.duration != "Total") {
                trendData.add(CollectionTrendResponse(row.duration, row.receivableAmount, row.collectableAmount, row.dashboardCurrency))
                monthList.add(row.duration)
                dashboardCurrency = row.dashboardCurrency
            } else {
                totalAmount = row.receivableAmount
                totalCollected = row.collectableAmount
                dashboardCurrency = row.dashboardCurrency
            }
        }
        getMonthFromQuarter(quarter).forEach {
            if (!monthList.contains(it)) {
                trendData.add(CollectionTrendResponse(it, 0.toBigDecimal(), 0.toBigDecimal(), "INR"))
            }
        }
        return CollectionResponse(totalAmount, totalCollected, trendData.sortedBy { Month.valueOf(it.duration!!.uppercase()) }, id, dashboardCurrency!!)
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
        var serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null
        zoneKey = if (zone.isNullOrBlank()) "ALL" else zone.uppercase()
        serviceTypeKey = if (serviceType?.name.equals(null) || (serviceType == ServiceType.NA)) "ALL" else serviceType?.name
        invoiceCurrencyKey = if (invoiceCurrency.isNullOrBlank()) "ALL" else invoiceCurrency.uppercase()
        return mapOf("zoneKey" to zoneKey, "serviceTypeKey" to serviceTypeKey, "invoiceCurrencyKey" to invoiceCurrencyKey)
    }

    override suspend fun generateArDashboardData (){
        val currMonth = AresConstants.CURR_MONTH
        val currYear = AresConstants.CURR_YEAR
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        val salesFunnelKey = AresConstants.SALES_FUNNEL_PREFIX + months[currMonth-1] + AresConstants.KEY_DELIMITER + currYear
        generatingSalesFunnelData(currMonth,currYear, salesFunnelKey)

        val startDate = AresConstants.CURR_DATE.toLocalDate()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate = "${AresConstants.CURR_YEAR}-${generateMonthKeyIndex(AresConstants.CURR_MONTH)}-${LocalDate.parse(startDate).month.length(LocalDate.parse(startDate).isLeapYear)}".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val invoiceTimeLineKey = AresConstants.INVOICE_TIME_LINE_PROFIX + startDate + AresConstants.KEY_DELIMITER + endDate
        generateInvoiceTimeline(startDate!!, endDate, invoiceTimeLineKey)

        val currentDate = java.util.Date.from(Instant.now())
        val outstandingIndexKey  = AresConstants.OUTSTANDING_PREFIX + currentDate + AresConstants.KEY_DELIMITER
        generateOutstandingData(currentDate, outstandingIndexKey)

    }

    override suspend fun generatingSalesFunnelData (monthKey: Int, year: Int, searchKey: String) {
        val monthKeyIndex = when (monthKey < 10) {
            true -> "0$monthKey"
            else -> monthKey.toString()
        }

        val startDate = "$year-$monthKeyIndex-01".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val convertedDate = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val convertedDateLength = convertedDate.month.length(convertedDate.isLeapYear)

        val endDate =
            "$year-$monthKeyIndex-${convertedDateLength}".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val salesFunnelResponse = SalesFunnelResponse()

        val data = unifiedDBRepo.getFunnelData(startDate, endDate)

        salesFunnelResponse.draftInvoicesCount = data?.size
        salesFunnelResponse.financeAcceptedInvoiceCount = data?.count { it.status?.name != "DRAFT" }
        salesFunnelResponse.irnGeneratedInvoicesCount =
            data?.count { !listOf("DRAFT", "FINANCE_ACCEPTED").contains(it.status?.name) }
        salesFunnelResponse.settledInvoicesCount = data?.count { it.paymentStatus == "PAID" }
        salesFunnelResponse.draftToFinanceAcceptedPercentage =
            salesFunnelResponse.financeAcceptedInvoiceCount?.times(100)
                ?.div(salesFunnelResponse.draftInvoicesCount!!)
        salesFunnelResponse.financeToIrnPercentage = salesFunnelResponse.irnGeneratedInvoicesCount?.times(100)
            ?.div(salesFunnelResponse.financeAcceptedInvoiceCount!!)
        salesFunnelResponse.settledPercentage = salesFunnelResponse.settledInvoicesCount?.times(100)
            ?.div(salesFunnelResponse.irnGeneratedInvoicesCount!!)

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, searchKey, salesFunnelResponse)
    }

    override suspend fun generateInvoiceTimeline(startDate: String, endDate: String, searchKey: String) {
        val data = unifiedDBRepo.getFunnelData(startDate, endDate)

        val mapOfData = mapOf("finance_accepted" to data?.filter {it.status?.name != "DRAFT"}, "irn_generated" to data?.filter { !listOf("DRAFT", "FINANCE_ACCEPTED").contains(it.status?.name) }, "settled" to  data?.filter { it.paymentStatus == "PAID" })
        val invoiceTimeLineResp = InvoiceTimeLineResponse()

        mapOfData.entries.map { (k,v) ->
            v?.map {invoice->
                val eventData = unifiedDBRepo.getInvoiceEvents(invoice.id!!)
                when (k) {
                    "finance_accepted" -> invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted = invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted?.plus(eventData?.first { it.eventName == "FINANCE_ACCEPTED" }?.createdAt?.time?.minus(
                        eventData.first { it.eventName == "CREATED" }.createdAt.time)?.div(1000)?.div(60)?.div(60)!!)
                    "irn_generated" -> invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated?.plus(eventData?.first { it.eventName == "IRN_GENERATED" }?.createdAt?.time?.minus(
                        eventData.first { it.eventName == "FINANCE_ACCEPTED" }.createdAt.time)?.div(1000)?.div(60)?.div(60)!!)
                    "settled" -> if (eventData?.first { it.eventName == "SETTLED" } != null)  {
                        invoiceTimeLineResp.tatHoursFromIrnGeneratedToSettled?.plus(eventData.first { it.eventName == "SETTLED" }.createdAt.time.minus(
                            eventData.first { it.eventName == "IRN_GENERATED" }.createdAt.time).div(1000).div(60).div(60))
                    } else {
                        invoiceTimeLineResp.tatHoursFromIrnGeneratedToSettled = 0L
                    }
                    else -> {}
                }
            }
        }

        invoiceTimeLineResp.draftInvoicesCount = data?.size
        invoiceTimeLineResp.financeAcceptedInvoiceCount = mapOfData["finance_accepted"]?.size
        invoiceTimeLineResp.irnGeneratedInvoicesCount = mapOfData["irn_generated"]?.size
        invoiceTimeLineResp.settledInvoicesCount = mapOfData["settled"]?.size

        if (invoiceTimeLineResp.financeAcceptedInvoiceCount!! != 0){
            invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted = invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted?.div(invoiceTimeLineResp.financeAcceptedInvoiceCount!!)
        }

        if (invoiceTimeLineResp.irnGeneratedInvoicesCount!! != 0){
            invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated?.div(invoiceTimeLineResp.irnGeneratedInvoicesCount!!)
        }
        if (invoiceTimeLineResp.settledInvoicesCount !=0 ){
            invoiceTimeLineResp.tatHoursFromIrnGeneratedToSettled?.div(invoiceTimeLineResp.settledInvoicesCount!!)
        }

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, searchKey, invoiceTimeLineResp)
    }

    override suspend fun generateOutstandingData(date: Date, searchKey: String) {
        val data = unifiedDBRepo.getOutstandingData(date)

        val mapData = hashMapOf<String, ServiceLevelOutstanding> ()

        data?.groupBy { it.groupedServices }?.filter { it.key != null }?.entries?.map {(k,v) ->
            mapData[k]?.totalOutstanding = v.sumOf { it.openInvoiceAmount }
            mapData[k]?.openInvoiceAmount = v.sumOf { it.openInvoiceAmount }
            mapData[k]?.onAccountPayment = BigDecimal.ZERO
            mapData[k]?.currency = v.first().currency
            mapData[k]?.tradeType= v.map { item ->
                TradeAndServiceLevelOutstanding(
                    key = "${item.serviceType}_${item.tradeType}",
                    name = "${item.serviceType} ${item.tradeType}".uppercase(),
                    totalOutstanding = item.openInvoiceAmount,
                    openInvoiceAmount = item.openInvoiceAmount,
                    onAccountPayment = BigDecimal.ZERO,
                    currency = item.currency
                )
            }
        }

        val outstandingOpensearchResponse = OutstandingOpensearchResponse(
            overallStats = OverallStats(
                totalOutstandingAmount = data?.sumOf { it.openInvoiceAmount },
                openInvoicesAmount = data?.sumOf { it.openInvoiceAmount },
                openOnAccountPaymentAmount = BigDecimal.ZERO,
                customersCount = data?.sumOf { it.customersCount!! },
                dashboardCurrency = data?.first()?.currency!!,
                openInvoicesCount = data.sumOf { it.openInvoicesCount!! }
            ),
            outstandingServiceWise = mapData
        )

        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, searchKey, outstandingOpensearchResponse)
    }

    override suspend fun getKamOrManagerWiseOutstandong() {
        TODO("Not yet implemented")
    }

    private fun generateMonthKeyIndex (month: Int): String{
        return when (month < 10 ){
            true -> "0${month}"
            else -> month.toString()
        }
    }
}
