package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.*
import com.cogoport.ares.api.payment.mapper.*
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.model.payment.*
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.*

@Singleton
class PushToClientServiceImpl : PushToClientService {

    private val currQuarter = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    private val currYear = Calendar.getInstance().get(Calendar.YEAR)
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

    override suspend fun pushDashboardData(zone: String?, date: String, quarter: Int?) {
        /** Collection Trend */
        val collectionZoneResponse = accountUtilizationRepository.generateCollectionTrend(zone, quarter)
        updateCollectionTrend(zone, quarter, collectionZoneResponse)
        val collectionResponseAll = accountUtilizationRepository.generateCollectionTrend(null, quarter)
        updateCollectionTrend(null, quarter, collectionResponseAll)

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
        updateDailySalesOutstanding(zone, dailySalesZoneData)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, date)
        updateDailySalesOutstanding(null, dailySalesAllData)

        /** Daily Payables Outstanding */
        val dailyPayablesZoneData = accountUtilizationRepository.generateDailyPayablesOutstanding(zone, date)
        updateDailyPayablesOutstanding(zone, dailyPayablesZoneData)
        val dailyPayablesAllData = accountUtilizationRepository.generateDailyPayablesOutstanding(null, date)
        updateDailyPayablesOutstanding(null, dailyPayablesAllData)

    }
    private fun updateCollectionTrend(zone: String?, quarter: Int?, data: MutableList<CollectionTrend>?) {
        val collectionData = mutableListOf<CollectionTrendResponse>()
        data?.forEach { collection ->
            run {
                collectionData.add(collectionTrendConverter.convertToModel(collection))
            }
        }
        val collectionId = if (zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "all_" + currYear +"_Q$quarter" else AresConstants.COLLECTIONS_TREND_PREFIX + zone + "_" + currYear + "_Q$quarter"
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionId, formatCollectionTrend(collectionData, collectionId))
    }
    private fun updateOverallStats(zone: String?, data: OverallStats) {
        val overallStatsData = overallStatsConverter.convertToModel(data)
        val statsId = if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "all" else AresConstants.OVERALL_STATS_PREFIX + zone
        overallStatsData.id = statsId
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, overallStatsData)
    }
    private fun updateMonthlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val monthlyTrendId = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "all" else AresConstants.MONTHLY_TREND_PREFIX + zone
        val monthlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach { outstanding ->
            monthlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyTrendId, MonthlyOutstanding(monthlyTrend, monthlyTrendId))
    }
    private fun updateQuarterlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val quarterlyTrendId = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "all" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        val quarterlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach { outstanding ->
            quarterlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId, QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId))
    }

    private fun updateDailySalesOutstanding(zone: String?, data: DailyOutstanding) {
        val dsoResponse = dsoConverter.convertToModel(data)
        val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + "all_" +dsoResponse.month + "_" + currYear else AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + zone + "_" + dsoResponse.month + "_" + currYear
           Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dsoResponse)
    }
    private fun updateDailyPayablesOutstanding(zone: String?, data: DailyOutstanding) {
        val dpoResponse = dpoConverter.convertToModel(data)
        val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + "all_" +dpoResponse.month + "_" + currYear else AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX + zone + "_" + dpoResponse.month + "_" + currYear
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, dpoResponse)
    }

    private fun formatCollectionTrend(data: MutableList<CollectionTrendResponse>, id: String): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: Float? = null
        var totalCollected: Float? = null
        for (row in data) {
            if (row.duration != "Total") {
                trendData.add(
                    CollectionTrendResponse(
                        row.duration,
                        row.receivableAmount,
                        row.collectableAmount
                    )
                )
            } else {
                totalAmount = row.receivableAmount
                totalCollected = row.collectableAmount
            }
        }
        return CollectionResponse(totalAmount, totalCollected, trendData, id)
    }
    /** Outstanding Data */
    override suspend fun pushOutstandingData(zone: String?, orgId: String) {
        val orgOutstandingZoneData = accountUtilizationRepository.generateOrgOutstanding(orgId, zone)
        updateOrgOutstanding(zone, currQuarter, orgId, orgOutstandingZoneData)
        val orgOutstandingAllData = accountUtilizationRepository.generateOrgOutstanding(orgId, null)
        updateOrgOutstanding(null, currQuarter, orgId, orgOutstandingAllData)
    }
    private fun updateOrgOutstanding(zone: String?, quarter: Int, orgId: String?, data: OrgOutstanding) {
        val orgOutstanding = orgOutstandingConverter.convertToModel(data)
        val orgOutstandingId = if(zone.isNullOrBlank()) orgId + "_all" + "_Q$quarter" else orgId +"_"+ zone + "_Q$quarter"
        Client.updateDocument(AresConstants.SALES_OUTSTANDING_INDEX, orgOutstandingId, orgOutstanding)
    }
}
