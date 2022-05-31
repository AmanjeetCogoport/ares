package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.model.CollectionRequest
import com.cogoport.ares.api.payment.model.DsoRequest
import com.cogoport.ares.api.payment.model.MonthlyOutstandingRequest
import com.cogoport.ares.api.payment.model.OutstandingAgeingRequest
import com.cogoport.ares.api.payment.model.OverallStatsRequest
import com.cogoport.ares.api.payment.model.QuarterlyOutstandingRequest
import com.cogoport.ares.api.payment.model.ReceivableRequest
import com.cogoport.ares.api.payment.model.SalesTrendRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsResponse
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
import com.cogoport.ares.model.payment.SalesTrend
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.SearchResponse

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
        }
        else if (year.toString().length != 4) {
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
        return outstandingResponse.map { overallAgeingConverter.convertToModel(it) }
    }

    override suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse? {
        validateInput(request.zone, request.role, request.quarter, request.year)
        val searchKey = searchKeyCollectionTrend(request)
        return OpenSearchClient().search(
            searchKey = searchKey,
            classType = CollectionResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        return if (request.zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + request.year + AresConstants.KEY_DELIMITER + "Q" + request.quarter else AresConstants.COLLECTIONS_TREND_PREFIX + request.zone + AresConstants.KEY_DELIMITER + request.year + AresConstants.KEY_DELIMITER + "Q" + request.quarter
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
        val searchKeySales = mutableListOf<String>()
        val searchKeyPayables = mutableListOf<String>()
        for (q in request.quarter) {
            searchKeyDailyOutstanding(request.zone, q, request.year, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX).forEach { key -> searchKeySales.add(key) }
            searchKeyDailyOutstanding(request.zone, q, request.year, AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX).forEach { key -> searchKeyPayables.add(key) }
        }
        val salesResponse = clientResponse(searchKeySales)
        val dsoList = mutableListOf<DsoResponse>()
        for (hts in salesResponse?.hits()?.hits()!!) {
            val data = hts.source()
            dsoList.add(DsoResponse(data!!.month, data.value))
        }

        val payablesResponse = clientResponse(searchKeyPayables)
        val dpoList = mutableListOf<DpoResponse>()
        for (hts in payablesResponse?.hits()?.hits()!!) {
            val data = hts.source()
            dpoList.add(DpoResponse(data!!.month, data.value))
        }

        val currentKey = searchKeyDailyOutstanding(request.zone, AresConstants.CURR_QUARTER, AresConstants.CURR_YEAR, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX)
        val currResponse = clientResponse(currentKey)
        var averageDso = 0.toFloat()
        var currentDso = 0.toFloat()
        for (hts in currResponse?.hits()?.hits()!!) {
            val data = hts.source()
            averageDso += data!!.value
            if (data.month == AresConstants.CURR_MONTH) {
                currentDso = currResponse.hits()!!.hits()[0].source()!!.value
            }
        }
        return DailySalesOutstanding(currentDso, averageDso / 3, dsoList.sortedBy { it.month }, dpoList.sortedBy { it.month })
    }

    private fun clientResponse(key: List<String>): SearchResponse<DailyOutstandingResponse>? {
        return OpenSearchClient().listApi(
            index = AresConstants.SALES_DASHBOARD_INDEX,
            classType = DailyOutstandingResponse::class.java,
            values = key)
    }

    private fun searchKeyDailyOutstanding(zone: String?, quarter: Int, year: Int, index: String): MutableList<String> {
        return when (quarter) {
            1 -> { generateKeyByMonth(listOf("1", "2", "3"), zone, year, index) }
            2 -> { generateKeyByMonth(listOf("4", "5", "6"), zone, year, index) }
            3 -> { generateKeyByMonth(listOf("7", "8", "9"), zone, year, index) }
            4 -> { generateKeyByMonth(listOf("10", "11", "12"), zone, year, index) }
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
                if (it.zone == request.zone) {
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

    override suspend fun getSalesTrend(request: SalesTrendRequest): MutableList<SalesTrend> {
        validateInput(request.zone, request.role)
        val totalSales = Client.search(
            { s ->
                s.index("index_invoices")
                    .size(0)
                    .aggregations("total_sales"){ a ->
                        a.dateHistogram { d -> d.field("invoiceDate").interval{ i -> i.time("month") } }
                            .aggregations("amount") { a ->
                                a.sum { s -> s.field("invoiceAmount") }
                            }
                    }
            },Void::class.java
        )
        val creditSales = Client.search(
            { s ->
                s.index("index_invoices")
                    .query { q ->
                        q.bool{ b ->
                            if (request.zone.isNullOrBlank()) {
                                b.must { t -> t.range { r -> r.field("creditDays").gt(JsonData.of(0)) } }
                            }
                            else {
                                b.must { t -> t.match { m -> m.field("zone").query(FieldValue.of(request.zone)) } }
                                b.must { t -> t.range { r -> r.field("creditDays").gt(JsonData.of(0)) } }
                            }
                        }
                    }
                    .size(0)

                    .aggregations("credit_sales"){ a ->
                        a.global { g -> g }
                        a.dateHistogram { d -> d.field("invoiceDate").interval{ i -> i.time("month") } }
                            .aggregations("amount") { a ->
                                a.sum { s -> s.field("invoiceAmount") }
                            }
                    }
            },Void::class.java
        )
        val totalSalesResponse = totalSales?.aggregations()?.get("total_sales")?.dateHistogram()?.buckets()?.array()!!.map { mapOf("key" to it.keyAsString(), "value" to it.aggregations()["amount"]?.sum()?.value()!!) }
        val creditSalesResponse = creditSales?.aggregations()?.get("credit_sales")?.dateHistogram()?.buckets()?.array()!!.map { mapOf("key" to it.keyAsString(), "value" to it.aggregations()["amount"]?.sum()?.value()!!) }
        val output = mutableListOf<SalesTrend>()
        for(t in totalSalesResponse){
            creditSalesResponse.forEach {
                if(it["key"] == t["key"]) {
                    output.add(
                        SalesTrend(
                            month = it["key"].toString(),
                            salesOnCredit = (it["value"].toString().toFloat() * 100) / t["value"].toString().toFloat()
                        ))

                }
            }
        }
        return if (output.size > 6) output.subList(output.size - 6,output.size) else output
    }
}
