package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.Quarter
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.ReceivableByAgeViaZone
import com.cogoport.ares.model.payment.Dso
import com.cogoport.ares.model.payment.SalesTrend
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.OverallStats
import com.cogoport.ares.model.payment.CollectionTrend
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.CollectionTrendResponse
import com.cogoport.ares.model.payment.OutstandingResponse
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    override suspend fun getOverallStats(zone: String?, role: String?): OverallStats? {
        validateInput(zone, role)
        val searchKey = searchKeyOverallStats(zone, role)

        return OpenSearchClient().response<OverallStats>(
            searchKey = searchKey,
            classType = OverallStats ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }
    private fun searchKeyOverallStats(zone: String?, role: String?): String {
        return if (zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "all" else AresConstants.OVERALL_STATS_PREFIX + zone
    }

    override suspend fun getSalesTrend(zone: String?, role: String?): SalesTrendResponse? {
        validateInput(zone, role)
        val searchKey = searchKeySalesTrend(zone, role)

        return OpenSearchClient().response<SalesTrendResponse>(
            searchKey = searchKey,
            classType = SalesTrendResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }
    private fun searchKeySalesTrend(zone: String?, role: String?): String {
        return if (zone.isNullOrBlank()) AresConstants.SALES_TREND_PREFIX + "all" else AresConstants.SALES_TREND_PREFIX + zone
    }

    private fun validateInput(zone: String?, role: String?) {
        if (AresConstants.ROLE_ZONE_HEAD == role && zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
    }

    private fun validateInput(zone: String?, role: String?, quarter: String) {
        if (quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER).size != 2) {
            throw AresException(AresError.ERR_1004, "")
        }
        validateInput(zone, role)
    }

    override suspend fun getOutStandingByAge(): List<AgeingBucket> {
        return paymentRepository.getAgeingBucket()
    }
    private fun generateDocKeysForMonth(zone: String?, role: String?, quarter: String): MutableList<String> {
        var keys = mutableListOf<String>()
        if (role.equals(AresConstants.ROLE_ZONE_HEAD)) {
            for (month in extractMonthsFromQuarter(quarter)) {
                keys.add(zone + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + extractYearFromQuarter(quarter) + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + month)
            }
        } else {
            for (month in extractMonthsFromQuarter(quarter)) {
                keys.add("all" + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + extractYearFromQuarter(quarter) + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + month)
            }
        }
        return keys
    }

    private fun generateDocKeysForQuarter(zone: String?, role: String?, quarter: String): String {
        var key: String
        if (role.equals(AresConstants.ROLE_ZONE_HEAD)) {
            key = zone + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + extractYearFromQuarter(quarter) + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[0]
        } else {
            key = "all" + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + extractYearFromQuarter(quarter) + AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER + quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[0]
        }
        return key
    }

    override suspend fun pushDataToOpenSearch() {
        val invoiceResponse = OverallStats(
            openInvoiceCount = 23,
            openInvoiceAmount = 23000.toBigDecimal(),
            onAccountPayment = 20000.toBigDecimal(),
            accountReceivables = 3000.toBigDecimal(),
            organizations = 100,
            docKey = AresConstants.OVERALL_STATS_PREFIX + "1"
        )

        val invoiceResponse2 = OverallStats(
            openInvoiceCount = 27,
            openInvoiceAmount = 12000.toBigDecimal(),
            onAccountPayment = 10000.toBigDecimal(),
            accountReceivables = 2000.toBigDecimal(),
            organizations = 100,
            docKey = AresConstants.OVERALL_STATS_PREFIX + "2"
        )

        val invoiceResponse1 = OverallStats(
            openInvoiceCount = 55,
            openInvoiceAmount = 35000.toBigDecimal(),
            onAccountPayment = 20000.toBigDecimal(),
            accountReceivables = 15000.toBigDecimal(),
            organizations = 200,
            docKey = AresConstants.OVERALL_STATS_PREFIX + "all"
        )

        val collectionTrend = CollectionTrend(
            totalReceivableAmount = 1000.toBigDecimal(),
            totalCollectedAmount = 2000.toBigDecimal(),
            trend = mutableListOf(
                CollectionTrendResponse(
                    duration = "jan",
                    receivableAmount = 10000.toBigDecimal(),
                    collectableAmount = 1000.toBigDecimal(),
                ),
                CollectionTrendResponse(
                    duration = "feb",
                    receivableAmount = 300.toBigDecimal(),
                    collectableAmount = 200.toBigDecimal(),
                )
            ),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX + "1_2022_Q2"
        )

        val collectionTrend1 = CollectionTrend(
            totalReceivableAmount = 3000.toBigDecimal(),
            totalCollectedAmount = 4000.toBigDecimal(),
            trend = mutableListOf(CollectionTrendResponse("mar", 10000.toBigDecimal(), 1000.toBigDecimal()), CollectionTrendResponse("april", 40000.toBigDecimal(), 900.toBigDecimal())),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX + "all_2022_Q2"
        )

        val collectionTrend2 = CollectionTrend(
            totalReceivableAmount = 5000.toBigDecimal(),
            totalCollectedAmount = 6000.toBigDecimal(),
            trend = mutableListOf(CollectionTrendResponse("may", 10000.toBigDecimal(), 1000.toBigDecimal()), CollectionTrendResponse("may", 50000.toBigDecimal(), 7000.toBigDecimal())),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX + "2_2022_Q2"
        )

        val monthlyOutstanding = MonthlyOutstanding(
            response = mutableListOf(OutstandingResponse("mar", 1000.toBigDecimal()), OutstandingResponse("apr", 2000.toBigDecimal())),
            docKey = AresConstants.MONTHLY_TREND_PREFIX + "all"
        )

        val monthlyOutstanding1 = MonthlyOutstanding(
            response = mutableListOf(OutstandingResponse("may", 100.toBigDecimal()), OutstandingResponse("jun", 200.toBigDecimal())),
            docKey = AresConstants.MONTHLY_TREND_PREFIX + "1"
        )

        val monthlyOutstanding2 = MonthlyOutstanding(
            response = mutableListOf(OutstandingResponse("jan", 1.toBigDecimal()), OutstandingResponse("feb", 2.toBigDecimal())),
            docKey = AresConstants.MONTHLY_TREND_PREFIX + "2"
        )

        val quarterlyOutstanding = QuarterlyOutstanding(
            response = mutableListOf(OutstandingResponse("jan", 1000.toBigDecimal()), OutstandingResponse("feb", 2000.toBigDecimal())),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX + "all"
        )

        val quarterlyOutstanding1 = QuarterlyOutstanding(
            response = mutableListOf(OutstandingResponse("mar", 1000.toBigDecimal()), OutstandingResponse("apr", 2000.toBigDecimal())),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX + "1"
        )

        val quarterlyOutstanding2 = QuarterlyOutstanding(
            response = mutableListOf(OutstandingResponse("may", 1000.toBigDecimal()), OutstandingResponse("jun", 2000.toBigDecimal())),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX + "2"
        )

        val salesTrendAll = SalesTrendResponse(
            response = listOf(SalesTrend("jan", 50.45), SalesTrend("Feb", 49.55)),
            docKey = AresConstants.SALES_TREND_PREFIX + "all"
        )
        val salesTrend1 = SalesTrendResponse(
            response = listOf(SalesTrend("jan", 35.80), SalesTrend("Feb", 64.20)),
            docKey = AresConstants.SALES_TREND_PREFIX + "1"
        )
        val salesTrend2 = SalesTrendResponse(
            response = listOf(SalesTrend("jan", 45.80), SalesTrend("Feb", 54.20)),
            docKey = AresConstants.SALES_TREND_PREFIX + "2"
        )
        val dailySalesOutstanding1 = DailySalesOutstanding(
            averageDsoForTheMonth = 30.toFloat(),
            averageDsoLast3Months = 45.toFloat(),
            dso = listOf(Dso("Jan", 50.00), Dso("Feb", 70.00)),
            docKey = "daily_sales_all_2022_Q1"
        )
        val dailySalesOutstanding2 = DailySalesOutstanding(
            averageDsoForTheMonth = 40.toFloat(),
            averageDsoLast3Months = 65.toFloat(),
            dso = listOf(Dso("Jan", 40.00), Dso("Feb", 80.00)),
            docKey = "daily_sales_zone_2022_Q1"
        )
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, invoiceResponse1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, invoiceResponse2)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, invoiceResponse)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionTrend)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionTrend2)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, collectionTrend1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyOutstanding)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyOutstanding2)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, monthlyOutstanding1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyOutstanding)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyOutstanding2)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, quarterlyOutstanding1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, salesTrendAll)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, salesTrend1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, salesTrend2)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesOutstanding1)
        Client.addDocument(AresConstants.SALES_DASHBOARD_INDEX, dailySalesOutstanding2)
    }

    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }

    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }

    override suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend? {
        validateInput(zone, role, quarter)
        val searchKey = AresConstants.COLLECTIONS_TREND_PREFIX + generateDocKeysForQuarter(zone, role, quarter)
        return OpenSearchClient().response<CollectionTrend>(
            searchKey = searchKey,
            classType = CollectionTrend ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }

    override suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "all" else AresConstants.MONTHLY_TREND_PREFIX + zone
        return OpenSearchClient().response<MonthlyOutstanding>(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }

    override suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "all" else AresConstants.QUARTERLY_TREND_PREFIX + zone
        return OpenSearchClient().response<QuarterlyOutstanding>(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }

    override suspend fun getDailySalesOutstanding(zone: String?, role: String?, quarter: String): DailySalesOutstanding? {
        validateInput(zone, role)
        val searchKey = AresConstants.DAILY_SALES_OUTSTANDING_PREFIX + generateDocKeysForQuarter(zone, role, quarter)
        return OpenSearchClient().response<DailySalesOutstanding>(
            searchKey = searchKey,
            classType = DailySalesOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX,
            key = AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }

    override suspend fun getReceivableByAge(zone: String?, role: String?): ReceivableAgeingResponse {

        var payment = accountUtilizationRepository.getReceivableByAge(zone)
        var receivableNorthBucket = mutableListOf<AgeingBucket>()
        var receivableSouthBucket = mutableListOf<AgeingBucket>()
        var receivableEastBucket = mutableListOf<AgeingBucket>()
        var receivableWestBucket = mutableListOf<AgeingBucket>()
        var receivableZoneBucket = mutableListOf<AgeingBucket>()
        var receivableByAgeViaZone = mutableListOf<ReceivableByAgeViaZone>()
        var zoneData = listOf<String>()

        if (zone.isNullOrBlank()) {
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

    private fun receivableBucketAllZone(response: com.cogoport.ares.api.payment.entity.AgeingBucketZone?): AgeingBucket {
        var receivableZoneBucket = mutableListOf<AgeingBucket>()

        return AgeingBucket(
            ageingDuration = response!!.ageingDuration,
            amount = response.amount,
            zone = null
        )
    }

    /**
     * Extract Months from Quarter
     * @param quarter : QQ-YYYY
     * @return MutableList : MMM,MMM,MMM
     */
    private fun extractMonthsFromQuarter(quarter: String): MutableList<String> {
        var quar = quarter.split("-")[0]
        return if (Quarter.Q1.quarter.equals(quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[0])) {
            Quarter.Q1.months
        } else if (Quarter.Q2.quarter.equals(quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[0])) {
            Quarter.Q2.months
        } else if (Quarter.Q3.quarter.equals(quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[0])) {
            Quarter.Q3.months
        } else if (Quarter.Q4.quarter.equals(quarter.split("_")[0])) {
            Quarter.Q4.months
        } else {
            throw AresException(AresError.ERR_1004, "")
        }
    }

    private fun extractYearFromQuarter(quarter: String): String {
        return quarter.split(AresConstants.OPEN_SEARCH_DOCUMENT_KEY_DELIMITER)[1]
    }
}
