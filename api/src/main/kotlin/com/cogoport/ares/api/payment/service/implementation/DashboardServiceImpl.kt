package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoResponse
import com.cogoport.ares.model.payment.DpoResponse
import com.cogoport.ares.model.payment.DailyOutstandingResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.AgeingBucketZone
import com.cogoport.ares.model.payment.ReceivableByAgeViaZone
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.*
@Singleton
class DashboardServiceImpl : DashboardService {
    private val currQuarter = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    private val currYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currMonth = Calendar.getInstance().get(Calendar.MONTH)

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper
    override suspend fun getOverallStats(zone: String?, role: String?): OverallStatsResponse? {
        validateInput(zone, role)
        val searchKey = searchKeyOverallStats(zone)
        return OpenSearchClient().response(
            searchKey = searchKey,
            classType = OverallStatsResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }
    private fun searchKeyOverallStats(zone: String?): String {
        return if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "all" else AresConstants.OVERALL_STATS_PREFIX + zone
    }
    override suspend fun getSalesTrend(zone: String?, role: String?): SalesTrendResponse? {
        validateInput(zone, role)
        val searchKey = searchKeySalesTrend(zone)

        return OpenSearchClient().response(
            searchKey = searchKey,
            classType = SalesTrendResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }
    private fun searchKeySalesTrend(zone: String?): String {
        return if (zone.isNullOrBlank()) AresConstants.SALES_TREND_PREFIX + "all" else AresConstants.SALES_TREND_PREFIX + zone
    }
    private fun validateInput(zone: String?, role: String?) {
        if (AresConstants.ROLE_ZONE_HEAD == role && zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
    }
    private fun validateInput(zone: String?, role: String?, quarter: Int) {
        if (quarter > 4 || quarter < 1) {
            throw AresException(AresError.ERR_1004, "")
        }
        validateInput(zone, role)
    }
    override suspend fun getOutStandingByAge(zone: String?, role: String?): MutableList<OverallAgeingStatsResponse> {
        validateInput(zone, role)
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(zone)
        val outstandingData = mutableListOf<OverallAgeingStatsResponse>()
        outstandingResponse.forEach { data ->
            run {
                outstandingData.add(overallAgeingConverter.convertToModel(data))
            }
        }
        return outstandingData
    }
    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }
    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }
    override suspend fun getCollectionTrend(zone: String?, role: String?, quarter: Int, year: Int): CollectionResponse? {
        validateInput(zone, role, quarter)
        val searchKey = searchKeyCollectionTrend(zone, quarter, year)
        return OpenSearchClient().response(
            searchKey = searchKey,
            classType = CollectionResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }
    private fun searchKeyCollectionTrend(zone: String?, quarter: Int, year: Int): String{
        return if(zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "all_" + year + "_Q$quarter" else AresConstants.COLLECTIONS_TREND_PREFIX + zone + "_" + year + "_Q$quarter"
    }
    override suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "all" else AresConstants.MONTHLY_TREND_PREFIX + zone
        return OpenSearchClient().response(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }
    override suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "all" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        return OpenSearchClient().response(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }
    override suspend fun getDailySalesOutstanding(zone: String?, role: String?, quarter: List<Int>, year: Int): DailySalesOutstanding {
        validateInput(zone, role)
        val searchKeySales = mutableListOf<String>()
        val searchKeyPayables = mutableListOf<String>()
        for (q in quarter){
            searchKeyDailyOutstanding(zone, q, year, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX).forEach { key -> searchKeySales.add(key) }
            searchKeyDailyOutstanding(zone, q, year, AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX).forEach { key -> searchKeyPayables.add(key) }
        }
        val salesResponse = clientResponse(searchKeySales)
        val dsoList = mutableListOf<DsoResponse>()
        for (hts in salesResponse?.hits()?.hits()!!) {
            val data = hts.source()
            dsoList.add(DsoResponse(data!!.month,data.value))
        }

        val payablesResponse = clientResponse(searchKeyPayables)
        val dpoList = mutableListOf<DpoResponse>()
        for (hts in payablesResponse?.hits()?.hits()!!) {
            val data = hts.source()
            dpoList.add(DpoResponse(data!!.month,data.value))
        }

        val currentKey = searchKeyDailyOutstanding(zone, currQuarter, currYear, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX)
        val currResponse = clientResponse(currentKey)
        var averageDso = 0.toFloat()
        var currentDso = 0.toFloat()
        for (hts in currResponse?.hits()?.hits()!!){
            val data = hts.source()
            averageDso += data!!.value
            if (data.month == currMonth.toString()){
                currentDso = currResponse.hits()!!.hits()[0].source()!!.value
            }
        }
        return DailySalesOutstanding(currentDso, averageDso/3, dsoList.sortedBy { it.month }, dpoList.sortedBy { it.month })
    }
    private fun clientResponse(key: List<String>): SearchResponse<DailyOutstandingResponse>? {
        val response =  Client.search(
            { s ->
                s.index(AresConstants.SALES_DASHBOARD_INDEX).query {
                        q-> q.ids { i-> i.values(key) }
                }.from(0).size(10)
            },
            DailyOutstandingResponse::class.java
        )
        if (response?.hits()?.total()?.value() == 0.toLong())
            throw AresException(AresError.ERR_1005, "")
        return response
    }
    private fun searchKeyDailyOutstanding(zone: String?, quarter: Int, year: Int, index: String): MutableList<String> {
        return when (quarter) {
            1 -> { generateKeyByMonth(listOf("1","2","3"), zone, year, index) }
            2 -> { generateKeyByMonth(listOf("4","5","6"), zone, year, index) }
            3 -> { generateKeyByMonth(listOf("7","8","9"), zone, year, index) }
            4 -> { generateKeyByMonth(listOf("10","11","12"), zone, year, index) }
            else -> { throw AresException(AresError.ERR_1004, "") }
        }
    }
    private fun generateKeyByMonth(monthList: List<String>, zone: String?, year: Int, index: String): MutableList<String>{
        val keyList = mutableListOf<String>()
        for (item in monthList) {
            keyList.add(
                if (zone.isNullOrBlank()) index + "all" + "_" + item + "_" + year.toString()
                else index + zone + "_" + item + "_" + year.toString()
            )
        }
        return keyList
    }
    override suspend fun getReceivableByAge(zone: String?, role: String?): ReceivableAgeingResponse {

        val payment = accountUtilizationRepository.getReceivableByAge(zone)
        val receivableNorthBucket = mutableListOf<AgeingBucketZone>()
        val receivableSouthBucket = mutableListOf<AgeingBucketZone>()
        val receivableEastBucket = mutableListOf<AgeingBucketZone>()
        val receivableWestBucket = mutableListOf<AgeingBucketZone>()
        val receivableZoneBucket = mutableListOf<AgeingBucketZone>()
        val receivableByAgeViaZone = mutableListOf<ReceivableByAgeViaZone>()
        var zoneData = listOf<String>()

        if (zone.isNullOrBlank()) {
            zoneData = zoneData + listOf("East", "West", "North", "South")
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "North", ageingBucket = receivableNorthBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "South", ageingBucket = receivableSouthBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "East", ageingBucket = receivableEastBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "West", ageingBucket = receivableWestBucket))

            payment.forEach {
                when (it.zone) {
                    "north" -> receivableNorthBucket.add(receivableBucketAllZone(it))
                    "south" -> receivableSouthBucket.add(receivableBucketAllZone(it))
                    "east" -> receivableEastBucket.add(receivableBucketAllZone(it))
                    "west" -> receivableWestBucket.add(receivableBucketAllZone(it))
                }
            }

            receivableByAgeViaZone[0].ageingBucket = receivableNorthBucket
            receivableByAgeViaZone[1].ageingBucket = receivableSouthBucket
            receivableByAgeViaZone[2].ageingBucket = receivableEastBucket
            receivableByAgeViaZone[3].ageingBucket = receivableWestBucket
        } else {
            zoneData = zoneData + listOf(zone.toString())
            receivableByAgeViaZone.add(
                ReceivableByAgeViaZone(
                    zoneName = zone,
                    ageingBucket = receivableZoneBucket
                )
            )
            payment.forEach {
                if (it.zone == zone) {
                    receivableZoneBucket.add(receivableBucketAllZone(it))
                }
            }
            receivableByAgeViaZone[0].ageingBucket = receivableZoneBucket
        }

        return ReceivableAgeingResponse(
            zone = zoneData,
            receivableByAgeViaZone = receivableByAgeViaZone
        )
    }
    private fun receivableBucketAllZone(response: com.cogoport.ares.api.payment.entity.AgeingBucketZone?): AgeingBucketZone {
        return AgeingBucketZone(
            ageingDuration = response!!.ageingDuration,
            amount = response.amount,
            zone = null
        )
    }
}
