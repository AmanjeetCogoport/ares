package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.model.payment.AgeingBucketZone
import com.cogoport.ares.model.payment.CollectionRequest
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.DailyOutstandingResponse
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DpoResponse
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.DsoResponse
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsRequest
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.ReceivableByAgeViaZone
import com.cogoport.ares.model.payment.ReceivableRequest
import com.cogoport.ares.model.payment.SalesTrendRequest
import com.cogoport.ares.model.payment.SalesTrend
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    private fun validateInput(zone: String?, role: String?) {
        if (AresConstants.ROLE_ZONE_HEAD == role && zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
    }

    private fun validateInput(zone: String?, role: String?, quarter: Int, year: Int) {
        if (quarter > 4 || quarter < 1) {
            throw AresException(AresError.ERR_1004, "")
        } else if (year.toString().length != 4) {
            throw AresException(AresError.ERR_1006, "")
        }
        validateInput(zone, role)
    }

    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }

    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }

    override suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponse? {
        validateInput(request.zone, request.role)
        val searchKey = searchKeyOverallStats(request)
        return OpenSearchClient().search(
            searchKey = searchKey,
            classType = OverallStatsResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }

    private fun searchKeyOverallStats(request: OverallStatsRequest): String {
        return if (request.zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "ALL" else AresConstants.OVERALL_STATS_PREFIX + request.zone
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        validateInput(request.zone, request.role)
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(request.zone)
        val data = mutableListOf<OverallAgeingStatsResponse>()
        outstandingResponse.map { data.add(overallAgeingConverter.convertToModel(it)) }
        val durationKey = listOf("1-30", "31-60", "61-90", ">90", "Not Due")
        val key = data.map { it.ageingDuration }
        durationKey.forEach {
            if (!key.contains(it)) {
                data.add(
                    OverallAgeingStatsResponse(it, 0.toBigDecimal(), "INR")
                )
            }
        }
        return data.sortedBy { it.ageingDuration }
    }

    override suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse? {
        validateInput(request.zone, request.role, request.quarterYear.split("_")[0][1].toString().toInt(), request.quarterYear.split("_")[1].toInt())
        val searchKey = searchKeyCollectionTrend(request)
        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = CollectionResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        return data ?: CollectionResponse(id = searchKey)
    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        return if (request.zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0] else AresConstants.COLLECTIONS_TREND_PREFIX + request.zone + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0]
    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding? {
        validateInput(request.zone, request.role)
        val searchKey = if (request.zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "ALL" else AresConstants.MONTHLY_TREND_PREFIX + request.zone
        return OpenSearchClient().search(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }

    override suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding? {
        validateInput(request.zone, request.role)
        val searchKey = if (request.zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "ALL" else AresConstants.QUARTERLY_TREND_PREFIX + request.zone
        return OpenSearchClient().search(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        validateInput(request.zone, request.role)
        val dsoList = mutableListOf<DsoResponse>()
        val dpoList = mutableListOf<DpoResponse>()
        val sortQuarterList = request.quarterYear.sortedBy { it.split("_")[1] + it.split("_")[0][1] }
        for (q in sortQuarterList) {
            val salesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_SALES_OUTSTANDING_PREFIX)
            val salesResponse = clientResponse(salesResponseKey)
            val dso = mutableListOf<DsoResponse>()
            for (hts in salesResponse?.hits()?.hits()!!) {
                val data = hts.source()
                dso.add(DsoResponse(data!!.month.toString(), data.value))
            }
            val monthListDso = dso.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDso.contains(it)) {
                    dso.add(DsoResponse(it, 0F))
                }
            }
            dso.sortedBy { it.month }.forEach { dsoList.add(it) }

            val payablesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX)
            val payablesResponse = clientResponse(payablesResponseKey)
            val dpo = mutableListOf<DpoResponse>()
            for (hts in payablesResponse?.hits()?.hits()!!) {
                val data = hts.source()
                dpo.add(DpoResponse(data!!.month.toString(), data.value))
            }
            val monthListDpo = dpo.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDpo.contains(it)) {
                    dpo.add(DpoResponse(it, 0F))
                }
            }
            dpo.sortedBy { it.month }.forEach { dpoList.add(it) }
        }
        val currentKey = searchKeyDailyOutstanding(request.zone, AresConstants.CURR_QUARTER, AresConstants.CURR_YEAR, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX)
        val currResponse = clientResponse(currentKey)
        var averageDso = 0.toFloat()
        var currentDso = 0.toFloat()
        for (hts in currResponse?.hits()?.hits()!!) {
            val data = hts.source()
            averageDso += data!!.value
            if (data.month == AresConstants.CURR_MONTH) {
                currentDso = hts.source()!!.value
            }
        }
        return DailySalesOutstanding(currentDso, averageDso / 3, dsoList.map { DsoResponse(Month.of(it.month.toInt()).toString(), it.dsoForTheMonth) }, dpoList.map { DpoResponse(Month.of(it.month.toInt()).toString(), it.dpoForTheMonth) })
    }

    private fun clientResponse(key: List<String>): SearchResponse<DailyOutstandingResponse>? {
        return OpenSearchClient().listApi(
            index = AresConstants.SALES_DASHBOARD_INDEX,
            classType = DailyOutstandingResponse::class.java,
            values = key
        )
    }

    private fun searchKeyDailyOutstanding(zone: String?, quarter: Int, year: Int, index: String): MutableList<String> {
        return generateKeyByMonth(getMonthFromQuarter(quarter), zone, year, index)
    }

    private fun getMonthFromQuarter(quarter: Int): List<String> {
        return when (quarter) {
            1 -> { listOf("1", "2", "3") }
            2 -> { listOf("4", "5", "6") }
            3 -> { listOf("7", "8", "9") }
            4 -> { listOf("10", "11", "12") }
            else -> { throw AresException(AresError.ERR_1004, "") }
        }
    }

    private fun generateKeyByMonth(monthList: List<String>, zone: String?, year: Int, index: String): MutableList<String> {
        val keyList = mutableListOf<String>()
        for (item in monthList) {
            keyList.add(
                if (zone.isNullOrBlank()) index + "ALL" + AresConstants.KEY_DELIMITER + item + AresConstants.KEY_DELIMITER + year
                else index + zone + AresConstants.KEY_DELIMITER + item + AresConstants.KEY_DELIMITER + year
            )
        }
        return keyList
    }

    override suspend fun getReceivableByAge(request: ReceivableRequest): ReceivableAgeingResponse {
        val payment = accountUtilizationRepository.getReceivableByAge(request.zone)
        val receivableNorthBucket = mutableListOf<AgeingBucketZone>()
        val receivableSouthBucket = mutableListOf<AgeingBucketZone>()
        val receivableEastBucket = mutableListOf<AgeingBucketZone>()
        val receivableWestBucket = mutableListOf<AgeingBucketZone>()
        val receivableZoneBucket = mutableListOf<AgeingBucketZone>()
        val receivableByAgeViaZone = mutableListOf<ReceivableByAgeViaZone>()
        var zoneData = listOf<String>()

        if (request.zone.isNullOrBlank()) {
            zoneData = zoneData + listOf("East", "West", "North", "South")
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "North", ageingBucket = receivableNorthBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "South", ageingBucket = receivableSouthBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "East", ageingBucket = receivableEastBucket))
            receivableByAgeViaZone.add(ReceivableByAgeViaZone(zoneName = "West", ageingBucket = receivableWestBucket))

            payment.forEach {
                when (it.zone) {
                    "NORTH" -> receivableNorthBucket.add(receivableBucketAllZone(it))
                    "SOUTH" -> receivableSouthBucket.add(receivableBucketAllZone(it))
                    "EAST" -> receivableEastBucket.add(receivableBucketAllZone(it))
                    "WEST" -> receivableWestBucket.add(receivableBucketAllZone(it))
                }
            }

            val res = AgeingBucketZone(ageingDuration = "", amount = 0.toBigDecimal(), zone = "")
            if (receivableNorthBucket.isEmpty()) {
                receivableNorthBucket.add(res)
            }
            if (receivableSouthBucket.isEmpty()) {
                receivableSouthBucket.add(res)
            }
            if (receivableEastBucket.isEmpty()) {
                receivableEastBucket.add(res)
            }
            if (receivableWestBucket.isEmpty()) {
                receivableWestBucket.add(res)
            }

            receivableByAgeViaZone[0].ageingBucket = receivableNorthBucket
            receivableByAgeViaZone[1].ageingBucket = receivableSouthBucket
            receivableByAgeViaZone[2].ageingBucket = receivableEastBucket
            receivableByAgeViaZone[3].ageingBucket = receivableWestBucket
        } else {
            zoneData = zoneData + listOf(request.zone.toString())
            receivableByAgeViaZone.add(
                ReceivableByAgeViaZone(
                    zoneName = request.zone,
                    ageingBucket = receivableZoneBucket
                )
            )
            payment.forEach {
                if (it.zone == request.zone) { receivableZoneBucket.add(receivableBucketAllZone(it)) }
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

    override suspend fun getSalesTrend(request: SalesTrendRequest): List<SalesTrend> {
        validateInput(request.zone, request.role)
        val startDate = LocalDate.now().minusMonths(6).atStartOfDay()
        val totalSalesResponse = OpenSearchClient().salesTrendTotalSales(request.zone, startDate)?.aggregations()?.get("total_sales")?.dateHistogram()?.buckets()?.array()!!.map { mapOf("key" to it.keyAsString(), "value" to it.aggregations()["amount"]?.sum()?.value()!!) }
        val creditSalesResponse = OpenSearchClient().salesTrendCreditSales(request.zone, startDate)?.aggregations()?.get("credit_sales")?.dateHistogram()?.buckets()?.array()!!.map { mapOf("key" to it.keyAsString(), "value" to it.aggregations()["amount"]?.sum()?.value()!!) }
        var output = mutableListOf<SalesTrend>()
        for (t in totalSalesResponse) {
            var add = true
            creditSalesResponse.forEach {
                if (it["key"] == t["key"]) {
                    add = false
                    output.add(
                        SalesTrend(
                            month = ZonedDateTime.parse(it["key"].toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")).month.toString(),
                            salesOnCredit = (it["value"].toString().toDouble() * 100) / t["value"].toString().toDouble()
                        )
                    )
                }
            }
            if (add) {
                output.add(
                    SalesTrend(
                        month = ZonedDateTime.parse(t["key"].toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")).month.toString(),
                        salesOnCredit = 0.toDouble()
                    )
                )
            }
        }
        output = if (output.size > 6) output.subList(0, 6) else output
        return output.map { if (it.salesOnCredit.isNaN()) SalesTrend(it.month, 0.toDouble()) else SalesTrend(it.month, it.salesOnCredit) }
    }
}
