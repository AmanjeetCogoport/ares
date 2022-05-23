package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.Dso
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.mapper.CollectionTrendMapper
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.api.payment.mapper.DsoMapper
import com.cogoport.ares.api.payment.mapper.OutstandingMapper
import com.cogoport.ares.api.payment.mapper.OverallStatsMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.model.payment.CollectionTrendResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.DsoResponse
import com.cogoport.ares.model.payment.OutstandingResponse
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.IsoFields

@Singleton
class PushToClientServiceImpl : PushToClientService {
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var dsoConverter: DsoMapper

    @Inject
    lateinit var collectionTrendConverter: CollectionTrendMapper

    @Inject
    lateinit var overallStatsConverter: OverallStatsMapper

    @Inject
    lateinit var outstandingConverter: OutstandingMapper

    override suspend fun pushDataToOpenSearch(zone: String?) {
        val quarter = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)

        /** Collection Trend */
        val collectionZoneResponse = accountUtilizationRepository.generateCollectionTrend(zone, quarter)
        updateCollectionTrend(zone, quarter, collectionZoneResponse)
        val collectionResponseAll = accountUtilizationRepository.generateCollectionTrend(null, quarter)
        updateCollectionTrend(null, quarter, collectionResponseAll)

        /** Overall Stats */
        var statsZoneData = accountUtilizationRepository.generateOverallStats(zone)
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
        val quarterlyTrendAllData = accountUtilizationRepository.generateQuarterlyOutstanding(zone)
        updateQuarterlyTrend(null, quarterlyTrendAllData)

        /** Daily Sales Outstanding*/
        val dailySalesZoneData = accountUtilizationRepository.generateDailySalesOutstanding(zone, quarter)
        updateDailySalesOutstanding(zone, quarter, dailySalesZoneData)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, quarter)
        updateDailySalesOutstanding(null, quarter, dailySalesAllData)
    }

    private fun updateCollectionTrend(zone: String?, quarter: Int, data: MutableList<CollectionTrend>?) {
        var collectionData = mutableListOf<CollectionTrendResponse>()
        data?.forEach { data ->
            run {
                collectionData.add(collectionTrendConverter.convertToModel(data))
            }
        }
        val collectionId =
            if (zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "all" + "_Q$quarter" else AresConstants.COLLECTIONS_TREND_PREFIX + zone + "_Q$quarter"
        Client.updateDocument(
            AresConstants.SALES_DASHBOARD_INDEX,
            collectionId,
            formatCollectionTrend(collectionData, collectionId)
        )
    }

    private fun updateOverallStats(zone: String?, data: OverallStats) {
        val overallStatsData = overallStatsConverter.convertToModel(data)
        val statsId =
            if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "all" else AresConstants.OVERALL_STATS_PREFIX + zone
        overallStatsData.id = statsId
        Client.updateDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, overallStatsData)
    }

    private fun updateMonthlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val monthlyTrendId =
            if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "all" else AresConstants.MONTHLY_TREND_PREFIX + zone
        var monthlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach { outstanding ->
            monthlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.updateDocument(
            AresConstants.SALES_DASHBOARD_INDEX,
            monthlyTrendId,
            MonthlyOutstanding(monthlyTrend, monthlyTrendId)
        )
    }

    private fun updateQuarterlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val quarterlyTrendId =
            if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "all" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        var quarterlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach { outstanding ->
            quarterlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.updateDocument(
            AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId,
            QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId)
        )
    }

    private fun updateDailySalesOutstanding(zone: String?, quarter: Int, dataArr: List<Dso>?) {
        var dsoResponses = mutableListOf<DsoResponse>()
        dataArr?.forEach { data ->
            run {
                dsoResponses.add(dsoConverter.convertToModel(data))
            }
        }
        val dailySalesId =
            if (zone.isNullOrBlank()) AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + "all" + "_Q$quarter" else AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + zone + "_Q$quarter"
        Client.updateDocument(
            AresConstants.SALES_DASHBOARD_INDEX,
            dailySalesId,
            mapOf("dso" to dsoResponses, "id" to dailySalesId)
        )
    }

    private fun formatCollectionTrend(data: MutableList<CollectionTrendResponse>, id: String): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
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
}
