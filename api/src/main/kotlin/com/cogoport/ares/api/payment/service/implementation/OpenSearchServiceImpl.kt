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
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.CollectionTrendResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Month

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

    override suspend fun pushDashboardData(request: OpenSearchRequest) {
        val zone = request.zone
        val quarter = request.quarter
        val date = request.date
        val year = request.year

        /** Collection Trend */
        val collectionZoneResponse = accountUtilizationRepository.generateCollectionTrend(zone, quarter)
        updateCollectionTrend(zone, quarter, year, collectionZoneResponse)
        val collectionResponseAll = accountUtilizationRepository.generateCollectionTrend(null, quarter)
        updateCollectionTrend(null, quarter, year, collectionResponseAll)

        /** Overall Stats */
        val statsZoneData = accountUtilizationRepository.generateOverallStats(zone)
        updateOverallStats(zone, statsZoneData)
        val statsAllData = accountUtilizationRepository.generateOverallStats(null)
        updateOverallStats(null, statsAllData)

        /** Monthly Outstanding */
        val monthlyTrendZoneData = accountUtilizationRepository.generateMonthlyOutstanding(zone)
        updateMonthlyTrend(zone, monthlyTrendZoneData)
        val monthlyTrendAllData = accountUtilizationRepository.generateMonthlyOutstanding(null)
        updateMonthlyTrend(null, monthlyTrendAllData)

        /** Quarterly Outstanding */
        val quarterlyTrendZoneData = accountUtilizationRepository.generateQuarterlyOutstanding(zone)
        updateQuarterlyTrend(zone, quarterlyTrendZoneData)
        val quarterlyTrendAllData = accountUtilizationRepository.generateQuarterlyOutstanding(null)
        updateQuarterlyTrend(null, quarterlyTrendAllData)

        /** Daily Sales Outstanding */
        val dailySalesZoneData = accountUtilizationRepository.generateDailySalesOutstanding(zone, date)
        updateDailySalesOutstanding(zone, year, dailySalesZoneData)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, date)
        updateDailySalesOutstanding(null, year, dailySalesAllData)

        /** Daily Payables Outstanding */
        val dailyPayablesZoneData = accountUtilizationRepository.generateDailyPayablesOutstanding(zone, date)
        updateDailyPayablesOutstanding(zone, year, dailyPayablesZoneData)
        val dailyPayablesAllData = accountUtilizationRepository.generateDailyPayablesOutstanding(null, date)
        updateDailyPayablesOutstanding(null, year, dailyPayablesAllData)
    }

    private fun updateCollectionTrend(zone: String?, quarter: Int, year: Int, data: MutableList<CollectionTrend>?) {
        if (data.isNullOrEmpty()) return
        val collectionData = data.map { collectionTrendConverter.convertToModel(it) }
        val collectionId = if (zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + year + AresConstants.KEY_DELIMITER + "Q$quarter" else AresConstants.COLLECTIONS_TREND_PREFIX + zone + AresConstants.KEY_DELIMITER + year + AresConstants.KEY_DELIMITER + "Q$quarter"
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionId, formatCollectionTrend(collectionData, collectionId, quarter))
    }

    private fun updateOverallStats(zone: String?, data: OverallStats) {
        val overallStatsData = overallStatsConverter.convertToModel(data)
        val statsId = if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "ALL" else AresConstants.OVERALL_STATS_PREFIX + zone
        overallStatsData.id = statsId
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, overallStatsData)
    }

    private fun updateMonthlyTrend(zone: String?, data: MutableList<Outstanding>?) {
        if (data.isNullOrEmpty()) return
        val monthlyTrend = data.map { outstandingConverter.convertToModel(it) }
        val monthlyTrendId = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "ALL" else AresConstants.MONTHLY_TREND_PREFIX + zone
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyTrendId, MonthlyOutstanding(monthlyTrend, monthlyTrendId))
    }

    private fun updateQuarterlyTrend(zone: String?, data: MutableList<Outstanding>?) {
        if (data.isNullOrEmpty()) return
        val quarterlyTrendId = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "ALL" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        val quarterlyTrend = data.map { outstandingConverter.convertToModel(it) }
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId, QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId))
    }

    private fun updateDailySalesOutstanding(zone: String?, year: Int, data: DailyOutstanding) {
        val dsoResponse = dsoConverter.convertToModel(data)
        val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + dsoResponse.month + AresConstants.KEY_DELIMITER + year else AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + zone + AresConstants.KEY_DELIMITER + dsoResponse.month + AresConstants.KEY_DELIMITER + year
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dsoResponse)
    }

    private fun updateDailyPayablesOutstanding(zone: String?, year: Int, data: DailyOutstanding) {
        val dpoResponse = dpoConverter.convertToModel(data)
        val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + dpoResponse.month + AresConstants.KEY_DELIMITER + year else AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + zone + AresConstants.KEY_DELIMITER + dpoResponse.month + AresConstants.KEY_DELIMITER + year
        OpenSearchClient().updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dpoResponse)
    }

    private fun formatCollectionTrend(data: List<CollectionTrendResponse>, id: String, quarter: Int): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
        val monthList = mutableListOf<String?>()
        for (row in data) {
            if (row.duration != "Total") {
                trendData.add(CollectionTrendResponse(row.duration, row.receivableAmount, row.collectableAmount))
                monthList.add(row.duration)
            } else {
                totalAmount = row.receivableAmount
                totalCollected = row.collectableAmount
            }
        }
        getMonthFromQuarter(quarter).forEach {
            if (!monthList.contains(it)) {
                trendData.add(CollectionTrendResponse(it, 0.toBigDecimal(), 0.toBigDecimal()))
            }
        }
        return CollectionResponse(totalAmount, totalCollected, trendData.sortedBy { Month.valueOf(it.duration!!.uppercase()) }, id)
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
        } else if (request.zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, request.zone).also {
            updateOrgOutstanding(request.zone, request.orgId, it)
        }
    }

    private fun updateOrgOutstanding(zone: String?, orgId: String?, data: List<OrgOutstanding>) {
        if (data.isEmpty()) return
        val dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
        val invoicesDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.openInvoicesAmount.toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
        val paymentsDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.paymentsAmount.toString().toBigDecimal() }, it.value.sumOf { it.paymentsCount!! }) }.toMutableList()
        val outstandingDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.outstandingAmount.toString().toBigDecimal() }, 0) }.toMutableList()
        val invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
        val paymentsCount = dataModel.sumOf { it.paymentsCount!! }
        val invoicesLedAmount = dataModel.sumOf { it.openInvoicesLedAmount!! }
        val paymentsLedAmount = dataModel.sumOf { it.paymentsLedAmount!! }
        val outstandingLedAmount = dataModel.sumOf { it.outstandingLedAmount!! }
        validateDueAmount(invoicesDues)
        validateDueAmount(paymentsDues)
        validateDueAmount(outstandingDues)
        val orgOutstanding = CustomerOutstanding(orgId, data[0].organizationName, zone, InvoiceStats(invoicesCount, invoicesLedAmount, invoicesDues), InvoiceStats(paymentsCount, paymentsLedAmount, paymentsDues), InvoiceStats(0, outstandingLedAmount, outstandingDues), null)
        OpenSearchClient().updateDocument(AresConstants.SALES_OUTSTANDING_INDEX, orgId!!, orgOutstanding)
    }

    private fun validateDueAmount(data: MutableList<DueAmount>) {
        listOf("INR", "USD").forEach { curr ->
            if (curr !in data.groupBy { it.currency }) {
                data.add(DueAmount(curr, 0.toBigDecimal(), 0))
            }
        }
    }
}
