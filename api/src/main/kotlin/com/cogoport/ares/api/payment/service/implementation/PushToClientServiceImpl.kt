package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.CollectionTrendResponse
import com.cogoport.ares.api.payment.entity.CollectionTrendModel
import com.cogoport.ares.api.payment.entity.OverallOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.api.payment.mapper.DsoMapper
import com.cogoport.ares.api.payment.mapper.OutstandingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.model.payment.Dso
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OutstandingResponse
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
    lateinit var outstandingConverter: OutstandingMapper

    override suspend fun pushDataToOpenSearch(zone: String?) {
        /** Collection Trend */
        val quarter = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
        val collectionZoneData = accountUtilizationRepository.generateCollectionTrend(zone, quarter)
        updateCollectionTrend(zone, quarter, collectionZoneData)
        val collectionAllData = accountUtilizationRepository.generateCollectionTrend(null, quarter)
        updateCollectionTrend(null, quarter, collectionAllData)

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
        val quarterlyTrendAllData = accountUtilizationRepository.generateQuarterlyOutstanding(zone)
        updateQuarterlyTrend(null, quarterlyTrendAllData)

        /** Daily Sales Outstanding*/
        val dailySalesZoneData = accountUtilizationRepository.generateDailySalesOutstanding(zone, quarter)
        var dsos = mutableListOf<Dso>()
        dailySalesZoneData.forEach { data ->
            run {
                dsos.add(dsoConverter.convertToModel(data))
            }
        }
        updateDailySalesOutstanding(zone, quarter, dsos)
        val dailySalesAllData = accountUtilizationRepository.generateDailySalesOutstanding(null, quarter)
        var dsosAll = mutableListOf<Dso>()
        dailySalesAllData.forEach { data ->
            run {
                dsosAll.add(dsoConverter.convertToModel(data))
            }
        }
        updateDailySalesOutstanding(null, quarter, dsosAll)
    }

    private fun updateCollectionTrend(zone: String?, quarter: Int, data: List<CollectionTrendModel>) {
        val collectionId = if (zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "all" + "_Q$quarter" else AresConstants.COLLECTIONS_TREND_PREFIX + zone + "_Q$quarter"
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionId, formatCollectionTrend(data, collectionId))
    }

    private fun updateOverallStats(zone: String?, data: OverallOutstanding) {
        val statsId = if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "all" else AresConstants.OVERALL_STATS_PREFIX + zone
        data.id = statsId
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, statsId, data)
    }

    private fun updateMonthlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val monthlyTrendId = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "all" else AresConstants.MONTHLY_TREND_PREFIX + zone
        var monthlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach {
            outstanding ->
            monthlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyTrendId, MonthlyOutstanding(monthlyTrend, monthlyTrendId))
    }

    private fun updateQuarterlyTrend(zone: String?, outstandings: MutableList<Outstanding>?) {
        val quarterlyTrendId = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "all" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        var quarterlyTrend = mutableListOf<OutstandingResponse>()
        outstandings?.forEach {
            outstanding ->
            quarterlyTrend.add(outstandingConverter.convertToModel(outstanding))
        }
        Client.addDocument(
            AresConstants.SALES_DASHBOARD_INDEX, quarterlyTrendId,
            QuarterlyOutstanding(quarterlyTrend, quarterlyTrendId)
        )
    }
    private fun updateDailySalesOutstanding(zone: String?, quarter: Int, data: List<Dso>) {
        val dailySalesId = if (zone.isNullOrBlank()) AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + "all" + "_Q$quarter" else AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + zone + "_Q$quarter"
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesId, mapOf("dso" to data, "id" to dailySalesId))
    }

    private fun formatCollectionTrend(data: List<CollectionTrendModel>, id: String): com.cogoport.ares.model.payment.CollectionTrend {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
        for (row in data) {
            if (row.month != "Total") {
                trendData.add(
                    CollectionTrendResponse(row.month, row.totalReceivableAmount, row.totalCollectedAmount)
                )
            } else {
                totalAmount = row.totalReceivableAmount
                totalCollected = row.totalCollectedAmount
            }
        }
        return com.cogoport.ares.model.payment.CollectionTrend(totalAmount, totalCollected, trendData, id)
    }
//    private fun formatDailySalesOutstanding(data: List<Dso>,id: String): DailySalesOutstanding{
//        var averageDso: Double? = null
//        var averageDsoOverall: Double? = null
//        if(data.isNullOrEmpty()) {
//            averageDsoOverall = 0.0.toDouble()
//            averageDso = 0.0.toDouble() }
//        else {
//            averageDso = data[0].dsoForTheMonth
//            averageDsoOverall = data.map{it.dsoForTheMonth}.average()
//        }
//        return DailySalesOutstanding(averageDso,averageDsoOverall,data,id)
//    }
}
