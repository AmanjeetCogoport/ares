package com.cogoport.ares.payment.service.implementation

import com.cogoport.ares.common.AresConstants
import com.cogoport.ares.common.classes.OpenSearch
import com.cogoport.ares.common.enum.Quarter
import com.cogoport.ares.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.payment.model.*
import com.cogoport.ares.payment.repository.PaymentRepository
import com.cogoport.ares.payment.service.interfaces.DashboardService
import com.cogoport.ares.utils.code.AresError
import com.cogoport.ares.utils.exception.AresException
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Client.search
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.util.function.Function

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    override suspend fun getOverallOutstanding(zone: String?, role: String?): OverallOutstandingStats? {
        validateInput(zone, role)
        val searchKey = searchKeyOverallOutstanding(zone, role)

        return OpenSearch().response<OverallOutstandingStats>(
            searchKey= searchKey,
            classType= OverallOutstandingStats ::class.java,
            index= AresConstants.SALES_DASHBOARD_INDEX,
            key= AresConstants.OPEN_SEARCH_DOCUMENT_KEY
        )
    }
    private fun searchKeyOverallOutstanding(zone: String?, role: String?): String {
        return if(zone.isNullOrBlank()) AresConstants.STATS_PREFIX+"all" else AresConstants.STATS_PREFIX+zone
    }

    private fun validateInput(zone: String?, role: String?){
        if(AresConstants.ROLE_ZONE_HEAD == role && zone.isNullOrBlank()){
            throw AresException(AresError.ERR_1003, "zone")
        }
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

    override suspend fun addMonthlyOutstandingTrend() {
        val invoiceResponse = OverallOutstandingStats(
            null,
            openInvoiceCount = 23,
            openInvoiceAmount = 23000.toBigDecimal(),
            onAccountPayment = 20000.toBigDecimal(),
            accountReceivables = 3000.toBigDecimal(),
            organizations = 100,
            docKey = AresConstants.STATS_PREFIX+"1"
        )

        val invoiceResponse2 = OverallOutstandingStats(
            null,
            openInvoiceCount = 27,
            openInvoiceAmount = 12000.toBigDecimal(),
            onAccountPayment = 10000.toBigDecimal(),
            accountReceivables = 2000.toBigDecimal(),
            organizations = 100,
            docKey = AresConstants.STATS_PREFIX+"2"
        )

        val invoiceResponse1 = OverallOutstandingStats(
            null,
            openInvoiceCount = 55,
            openInvoiceAmount = 35000.toBigDecimal(),
            onAccountPayment = 20000.toBigDecimal(),
            accountReceivables = 15000.toBigDecimal(),
            organizations = 200,
            docKey = AresConstants.STATS_PREFIX+"all"
        )

        val collectionTrend = CollectionTrend(
            totalAmount = mapOf<String, BigDecimal>("receivables" to 10000.toBigDecimal(), "collections" to 2000.toBigDecimal()),
            month1 = mapOf<String, BigDecimal>("receivables" to 4000.toBigDecimal(), "collections" to 1000.toBigDecimal()),
            month2 = mapOf<String, BigDecimal>("receivables" to 3000.toBigDecimal(), "collections" to 500.toBigDecimal()),
            month3 = mapOf<String, BigDecimal>("receivables" to 3000.toBigDecimal(), "collections" to 500.toBigDecimal()),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX+"1_2022_Q2"
        )

        val collectionTrend1 = CollectionTrend(
            totalAmount = mapOf<String, BigDecimal>("receivables" to 12000.toBigDecimal(), "collections" to 3000.toBigDecimal()),
            month1 = mapOf<String, BigDecimal>("receivables" to 6000.toBigDecimal(), "collections" to 1000.toBigDecimal()),
            month2 = mapOf<String, BigDecimal>("receivables" to 3000.toBigDecimal(), "collections" to 1000.toBigDecimal()),
            month3 = mapOf<String, BigDecimal>("receivables" to 3000.toBigDecimal(), "collections" to 1000.toBigDecimal()),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX+"all_2022_Q2"
        )

        val collectionTrend2 = CollectionTrend(
            totalAmount = mapOf<String, BigDecimal>("receivables" to 2000.toBigDecimal(), "collections" to 1000.toBigDecimal()),
            month1 = mapOf<String, BigDecimal>("receivables" to 1000.toBigDecimal(), "collections" to 500.toBigDecimal()),
            month2 = mapOf<String, BigDecimal>("receivables" to 500.toBigDecimal(), "collections" to 250.toBigDecimal()),
            month3 = mapOf<String, BigDecimal>("receivables" to 500.toBigDecimal(), "collections" to 250.toBigDecimal()),
            docKey = AresConstants.COLLECTIONS_TREND_PREFIX+"2_2022_Q2"
        )

        val monthlyOutstanding = MonthlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan" to 10000.toBigDecimal(), "Feb" to 2000.toBigDecimal(),"Mar" to 11000.toBigDecimal(), "Apr" to 12000.toBigDecimal()),
            docKey = AresConstants.MONTHLY_TREND_PREFIX+"all"
        )

        val monthlyOutstanding1 = MonthlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan" to 8000.toBigDecimal(), "Feb" to 1000.toBigDecimal(),"Mar" to 7000.toBigDecimal(), "Apr" to 8000.toBigDecimal()),
            docKey = AresConstants.MONTHLY_TREND_PREFIX+"1"
        )

        val monthlyOutstanding2 = MonthlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan" to 2000.toBigDecimal(), "Feb" to 1000.toBigDecimal(),"Mar" to 4000.toBigDecimal(), "Apr" to 4000.toBigDecimal()),
            docKey = AresConstants.MONTHLY_TREND_PREFIX+"2"
        )

        val quarterlyOutstanding = QuarterlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan-Mar" to 10000.toBigDecimal(), "Apr-Jun" to 2000.toBigDecimal(),"Jul-Sep" to 11000.toBigDecimal(), "Sep-Dec" to 12000.toBigDecimal()),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX+"all"
        )

        val quarterlyOutstanding1 = QuarterlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan-Mar" to 8000.toBigDecimal(), "Apr-Jun" to 1000.toBigDecimal(),"Jul-Sep" to 7000.toBigDecimal(), "Sep-Dec" to 8000.toBigDecimal()),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX+"1"
        )

        val quarterlyOutstanding2 = QuarterlyOutstanding(
            trend = mapOf<String, BigDecimal>("Jan-Mar" to 2000.toBigDecimal(), "Apr-Jun" to 1000.toBigDecimal(),"Jul-Sep" to 4000.toBigDecimal(), "Sep-Dec" to 4000.toBigDecimal()),
            docKey = AresConstants.QUARTERLY_TREND_PREFIX+"2"
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
    }

    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }

    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }

    override suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend? {
        validateInput(zone, role)
        val searchKey = AresConstants.COLLECTIONS_TREND_PREFIX+generateDocKeysForQuarter(zone, role, quarter)
        val response = search(
            Function { s: SearchRequest.Builder ->
                s.index(AresConstants.SALES_DASHBOARD_INDEX)
                    .query { q: Query.Builder ->
                        q.match { t: MatchQuery.Builder ->
                            t.field(AresConstants.OPEN_SEARCH_DOCUMENT_KEY).query(FieldValue.of(searchKey))
                        }
                    }
            },
            CollectionTrend::class.java
        )
        var outResp: CollectionTrend? = null
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }

    override suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX+"all" else AresConstants.MONTHLY_TREND_PREFIX+zone
        val response = search(
            Function { s: SearchRequest.Builder ->
                s.index(AresConstants.SALES_DASHBOARD_INDEX)
                    .query { q: Query.Builder ->
                        q.match { t: MatchQuery.Builder ->
                            t.field(AresConstants.OPEN_SEARCH_DOCUMENT_KEY).query(FieldValue.of(searchKey))
                        }
                    }
            },
            MonthlyOutstanding::class.java
        )

        var outResp: MonthlyOutstanding? = null
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }

    override suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding? {
        validateInput(zone, role)
        val searchKey = if (zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX+"all" else AresConstants.QUARTERLY_TREND_PREFIX+zone
        val response = search(
            Function { s: SearchRequest.Builder ->
                s.index(AresConstants.SALES_DASHBOARD_INDEX)
                    .query { q: Query.Builder ->
                        q.match { t: MatchQuery.Builder ->
                            t.field(AresConstants.OPEN_SEARCH_DOCUMENT_KEY).query(FieldValue.of(searchKey))
                        }
                    }
            },
            QuarterlyOutstanding::class.java
        )

        var outResp: QuarterlyOutstanding? = null
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
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
