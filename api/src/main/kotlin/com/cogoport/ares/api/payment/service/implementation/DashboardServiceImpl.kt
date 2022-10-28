package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ExchangeRequestPeriod
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.ExchangeClient
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AgeingBucketZone
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.PayableAgeingBucket
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.CollectionRequest
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.DailyOutstandingResponse
import com.cogoport.ares.model.payment.response.DpoResponse
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import com.cogoport.ares.model.payment.response.PayableOutstandingResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    @Inject
    lateinit var openSearchService: OpenSearchService

//    @Inject
//    lateinit var exchangeClient: ExchangeClient

    @Inject
    lateinit var exchangeRateHelper: ExchangeRateHelper


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
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val dashboardCurrency = request.dashboardCurrency
        val quarter: Int = AresConstants.CURR_QUARTER
        val year: Int = AresConstants.CURR_YEAR

        validateInput(zone, request.role)
        val searchKey = searchKeyOverallStats(request)

        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = OverallStatsResponse::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data == null) {
            openSearchService.generateOverallStats(zone, quarter, year, serviceType, invoiceCurrency, dashboardCurrency!!)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = OverallStatsResponse ::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }
        if ((request.dashboardCurrency != data?.dashboardCurrency) and (data?.dashboardCurrency != null)) {
            val requestExchangeRate = ArrayList<String>()
            requestExchangeRate.add(data?.dashboardCurrency!!)
            val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(requestExchangeRate, request.dashboardCurrency!!)
            val avgExchangeRate = exchangeRate[data?.dashboardCurrency]

            data.totalOutstandingAmount = data.totalOutstandingAmount.times(avgExchangeRate!!)
            data.openInvoicesAmount = data.openInvoicesAmount.times(avgExchangeRate)
            data.openOnAccountPaymentAmount = data.openOnAccountPaymentAmount.times(avgExchangeRate)
            data.dashboardCurrency = request.dashboardCurrency!!
        }

        return data ?: OverallStatsResponse(id = searchKey, dashboardCurrency = request.dashboardCurrency!!)
    }

    private fun searchKeyOverallStats(request: OverallStatsRequest): String {
        var keyMap = generatingOpenSearchKey(request.zone, request.serviceType, request.invoiceCurrency)
        return AresConstants.OVERALL_STATS_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]
    }

    private fun generatingOpenSearchKey(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?): Map<String, String?> {
        var zoneKey: String? = null
        var serviceTypeKey: String? = null
        var invoiceCurrencyKey: String? = null

        zoneKey = if (zone.isNullOrBlank()) "ALL" else zone?.uppercase()
        serviceTypeKey = if (serviceType?.name.equals(null)) "ALL" else serviceType.toString()
        invoiceCurrencyKey = if (invoiceCurrency.isNullOrBlank()) "ALL" else invoiceCurrency?.uppercase()
        return mapOf("zoneKey" to zoneKey, "serviceTypeKey" to serviceTypeKey, "invoiceCurrencyKey" to invoiceCurrencyKey)
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        validateInput(request.zone, request.role)
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(request.zone, request.serviceType, request.invoiceCurrency)
        val data = mutableListOf<OverallAgeingStatsResponse>()
        var formattedData = mutableListOf<OverallAgeingStatsResponse>()
        val uniqueCurrencyList: List<String> = outstandingResponse.map { it.dashboardCurrency!! }.distinct()

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)

        outstandingResponse.map { response ->
            if (response.dashboardCurrency != request.dashboardCurrency) {
                var avgExchangeRate = exchangeRate[response.dashboardCurrency]
                response.amount = response.amount.times(avgExchangeRate!!)
                response.dashboardCurrency = request.dashboardCurrency
            }
            data.add(overallAgeingConverter.convertToModel(response))
        }
        val durationKey = listOf("1-30", "31-60", "61-90", ">90", "Not Due")

        data.map { item ->
            val index = formattedData.indexOfFirst { (it.ageingDuration?.equals(item.ageingDuration))!! }
            if (index == -1) {
                formattedData?.add(item)
            } else {
                formattedData?.get(index).amount == formattedData?.get(index).amount?.plus(item?.amount!!)
            }
        }

        val key = formattedData.map { it.ageingDuration }
        durationKey.forEach {
            if (!key.contains(it)) {
                formattedData.add(
                    OverallAgeingStatsResponse(
                        it,
                        0.toBigDecimal(),
                        request.dashboardCurrency
                    )
                )
            }
        }
        formattedData = formattedData.sortedBy { it.ageingDuration }.toMutableList()
        formattedData.add(0, formattedData.removeAt(4))
        return formattedData
    }

    override suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse {
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val quarter = request.quarterYear.split("_")[0][1].toString().toInt()
        val year = request.quarterYear.split("_")[1].toInt()

        validateInput(zone, request.role, quarter, year)
        val searchKey = searchKeyCollectionTrend(request)
        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = CollectionResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        if (data == null) {
            openSearchService.generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = CollectionResponse ::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        if (data?.dashboardCurrency != request.dashboardCurrency) {
            val requestExchangeRate: List<String> = data?.trend?.map { it.dashboardCurrency!! }?.distinct()!!
            val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(requestExchangeRate, request.dashboardCurrency)
            val avgExchangeRate = exchangeRate[data?.dashboardCurrency]

            data.totalReceivableAmount = data.totalReceivableAmount?.times(avgExchangeRate!!)
            data.totalCollectedAmount = data.totalCollectedAmount?.times(avgExchangeRate!!)
            data.dashboardCurrency = request.dashboardCurrency

            data.trend?.forEach {
                if (it.dashboardCurrency != request.dashboardCurrency) {
                    val avgTrendExchangeRate = exchangeRate[it.dashboardCurrency]
                    it.collectableAmount = it.collectableAmount.times(avgTrendExchangeRate!!)
                    it.receivableAmount = it.receivableAmount.times(avgTrendExchangeRate)
                    it.dashboardCurrency = request.dashboardCurrency
                }
            }
        }
        return data ?: CollectionResponse(id = searchKey, dashboardCurrency = request.dashboardCurrency)
    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        var keyMap = generatingOpenSearchKey(request.zone, request.serviceType, request.invoiceCurrency)
        return AresConstants.COLLECTIONS_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0]
    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding {
        var zone = request.zone
        var serviceType = request.serviceType
        var invoiceCurrency = request.invoiceCurrency
        val quarter: Int = AresConstants.CURR_QUARTER
        val year: Int = AresConstants.CURR_YEAR

        validateInput(request.zone, request.role)

        var keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)

        val searchKey = AresConstants.MONTHLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]

        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data == null) {
            openSearchService.generateMonthlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = MonthlyOutstanding ::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val uniqueCurrencyList: List<String> = data?.list?.map { it.dashboardCurrency }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()
        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        data.list?.forEach { outstandingRes ->
            if ((outstandingRes.dashboardCurrency != request.dashboardCurrency) && (outstandingRes.dashboardCurrency != null)) {
                val avgExchangeRate = exchangeRate.get(outstandingRes.dashboardCurrency)
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        val newData = getMonthlyOutStandingData(data)

        val newMonthlyOutstanding = MonthlyOutstanding(
            list = newData,
            id = searchKey
        )

        return newMonthlyOutstanding ?: MonthlyOutstanding(id = searchKey)
    }

    private fun getMonthlyOutStandingData(data: MonthlyOutstanding?): List<OutstandingResponse>? {
        val listOfOutStanding: List<OutstandingResponse>? = data?.list?.groupBy { it.duration }?.values?.map {
            val outstandingData = OutstandingResponse(
                amount = it.sumOf { it.amount },
                duration = it.first().duration,
                dashboardCurrency = it.first().dashboardCurrency,
            )
            return@map outstandingData
        }

        return listOfOutStanding
    }

    private fun getQuarterlyOutStandingData(data: QuarterlyOutstanding?): List<OutstandingResponse>? {
        val listOfOutStanding: List<OutstandingResponse>? = data?.list?.groupBy { it.duration }?.values?.map {
            val outstandingData = OutstandingResponse(
                amount = it.sumOf { it.amount },
                duration = it.first().duration,
                dashboardCurrency = it.first().dashboardCurrency
            )
            return@map outstandingData
        }
        return listOfOutStanding
    }

    override suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding {
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val quarter: Int = AresConstants.CURR_QUARTER
        val year: Int = AresConstants.CURR_YEAR

        validateInput(zone, request.role)

        var keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)

        val searchKey = AresConstants.QUARTERLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]

        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data == null) {
            openSearchService.generateQuarterlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency)
            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = QuarterlyOutstanding ::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val uniqueCurrencyList: List<String> = data?.list?.map { it.dashboardCurrency }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()
        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        data?.list?.forEach { outstandingRes ->
            if ((outstandingRes.dashboardCurrency != request.dashboardCurrency) && (outstandingRes.dashboardCurrency != null)) {
                val avgExchangeRate = exchangeRate?.get(outstandingRes.dashboardCurrency)
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        var newData = getQuarterlyOutStandingData(data)

        var newQuarterlyOutstanding = QuarterlyOutstanding(
            list = newData,
            id = searchKey
        )

        return newQuarterlyOutstanding ?: QuarterlyOutstanding(id = searchKey)
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        validateInput(request.zone, request.role)
        val dsoList = mutableListOf<DsoResponse>()
        val dpoList = mutableListOf<DpoResponse>()
        var dashboardCurrency: String? = null
        val sortQuarterList = request.quarterYear.sortedBy { it.split("_")[1] + it.split("_")[0][1] }
        for (q in sortQuarterList) {
            val salesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_SALES_OUTSTANDING_PREFIX, request.serviceType, request.invoiceCurrency)
            var salesResponse = clientResponse(salesResponseKey)

            if (salesResponse!!.hits().hits().isNullOrEmpty()) {
                val date = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                openSearchService.generateDailySalesOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), request.serviceType, request.invoiceCurrency, date)
                salesResponse = clientResponse(salesResponseKey)
            }
            val dso = mutableListOf<DsoResponse>()
            for (hts in salesResponse!!.hits().hits()) {
                val data = hts.source()
                dso.add(DsoResponse(data!!.month.toString(), data.value, data.dashboardCurrency!!))
            }
            val monthListDso = dso.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDso.contains(it)) {
                    dso.add(DsoResponse(it, 0.toBigDecimal(), request.dashboardCurrency))
                }
            }
            dso.sortedBy { it.month }.forEach { dsoList.add(it) }

            val payablesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX, request.serviceType, request.invoiceCurrency)
            var payablesResponse = clientResponse(payablesResponseKey)

            if (payablesResponse == null) {
                val date = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                openSearchService.generateDailySalesOutstanding(request.zone!!, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), request.serviceType!!, request.invoiceCurrency!!, date)
                payablesResponse = clientResponse(payablesResponseKey)
            }

            val dpo = mutableListOf<DpoResponse>()
            if (!payablesResponse!!.hits().hits().isNullOrEmpty()) {
                for (hts in payablesResponse.hits().hits()) {
                    val data = hts.source()
                    dpo.add(DpoResponse(data!!.month.toString(), data.value, data.dashboardCurrency!!))
                }
            }

            val monthListDpo = dpo.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDpo.contains(it)) {
                    dpo.add(DpoResponse(it, 0.toBigDecimal(), "INR"))
                }
            }
            dpo.sortedBy { it.month }.forEach { dpoList.add(it) }
        }

        val currentKey = searchKeyDailyOutstanding(request.zone, AresConstants.CURR_QUARTER, AresConstants.CURR_YEAR, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX, request.serviceType, request.invoiceCurrency)
        var currResponse = clientResponse(currentKey)
        var averageDso = 0.toFloat()

        var currentDso = 0.toFloat()

        if (currResponse == null) {
            val date = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            openSearchService.generateDailySalesOutstanding(request.zone!!, AresConstants.CURR_QUARTER, AresConstants.CURR_YEAR, request.serviceType!!, request.invoiceCurrency!!, date)
            currResponse = clientResponse(currentKey)
        }

        for (hts in currResponse!!.hits().hits()) {
            val data = hts.source()
            averageDso += data!!.value.toFloat()
            if (data.month == AresConstants.CURR_MONTH) {
                currentDso = hts.source()!!.value.toFloat()
                dashboardCurrency = hts.source()!!.dashboardCurrency
            }
        }

        val dsoResponseData = dsoList.map {
            DsoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dsoForTheMonth, it.dashboardCurrency)
        }

        val dpoResponseData = dpoList.map {
            DpoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dpoForTheMonth, it.dashboardCurrency)
        }

        var avgDsoAmount = (averageDso / 3).toBigDecimal()

        if ((request.dashboardCurrency != dashboardCurrency) && (dashboardCurrency != null)) {

            var uniqueCurrencyList: List<String> = dsoResponseData.map { it.dashboardCurrency }.distinct() + dpoResponseData.map { it.dashboardCurrency }.distinct() + dashboardCurrency
            uniqueCurrencyList = uniqueCurrencyList.map { it }.distinct()

            val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)
            val avgExchangeRate = exchangeRate[dashboardCurrency]
            currentDso = currentDso.toBigDecimal().times(avgExchangeRate!!).toFloat()
            avgDsoAmount = avgDsoAmount.times(avgExchangeRate)

            dsoResponseData.forEach { dsoResponse ->
                if ((dsoResponse.dashboardCurrency != request.dashboardCurrency) && (dsoResponse.dashboardCurrency != null)) {
                    var avgExchangeRate = exchangeRate?.get(dsoResponse.dashboardCurrency)
                    dsoResponse.dsoForTheMonth = dsoResponse.dsoForTheMonth.times(avgExchangeRate!!)
                    dsoResponse.dashboardCurrency = request.dashboardCurrency
                }
            }

            dpoResponseData.forEach { dpoResponse ->
                if ((dpoResponse.dashboardCurrency != request.dashboardCurrency) && (dpoResponse.dashboardCurrency != null)) {
                    var avgExchangeRate = exchangeRate?.get(dpoResponse.dashboardCurrency)
                    dpoResponse.dpoForTheMonth = dpoResponse.dpoForTheMonth.times(avgExchangeRate!!)
                    dpoResponse.dashboardCurrency = request.dashboardCurrency
                }
            }
        }
        return DailySalesOutstanding(currentDso.toBigDecimal(), avgDsoAmount, dsoResponseData, dpoResponseData, request.serviceType?.name, request.dashboardCurrency)
    }

    private fun clientResponse(key: List<String>): SearchResponse<DailyOutstandingResponse>? {
        return OpenSearchClient().listApi(
            index = AresConstants.SALES_DASHBOARD_INDEX,
            classType = DailyOutstandingResponse::class.java,
            values = key
        )
    }

    private fun searchKeyDailyOutstanding(zone: String?, quarter: Int, year: Int, index: String, serviceType: ServiceType?, invoiceCurrency: String?): MutableList<String> {
        return generateKeyByMonth(getMonthFromQuarter(quarter), zone, year, index, serviceType, invoiceCurrency)
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

    private fun generateKeyByMonth(monthList: List<String>, zone: String?, year: Int, index: String, serviceType: ServiceType?, invoiceCurrency: String?): MutableList<String> {
        val keyList = mutableListOf<String>()
        for (item in monthList) {
            var keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
            keyList.add(index + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + item + AresConstants.KEY_DELIMITER + year)
        }
        return keyList
    }

    override suspend fun getReceivableByAge(request: ReceivableRequest): HashMap<String, ArrayList<AgeingBucketZone>> {
        val serviceType: ServiceType? = request.serviceType
        val invoiceCurrency: String? = request.invoiceCurrency
        val payments = accountUtilizationRepository.getReceivableByAge(request.zone, serviceType, invoiceCurrency)
        val data = HashMap<String, ArrayList<AgeingBucketZone>>()

        val uniqueCurrencyList: List<String> = payments.map { it.dashboardCurrency!! }.distinct()

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)

        payments.forEach { payment ->
            val zone = payment.zone
            val arrayListAgeingBucketZone = ArrayList<AgeingBucketZone>()

            if (payment.dashboardCurrency != request.dashboardCurrency) {
                val avgExchangeRate = exchangeRate.get(payment.dashboardCurrency)
                payment.amount = payment.amount.times(avgExchangeRate!!)
                payment.dashboardCurrency = request.dashboardCurrency
            }

            val ageingBucketData = AgeingBucketZone(
                ageingDuration = payment.ageingDuration,
                amount = payment.amount,
                dashboardCurrency = payment.dashboardCurrency
            )

            if (data.keys.contains(zone)) {
                val zoneWiseData = data[zone]

                val index = zoneWiseData?.indexOfFirst { it.ageingDuration == payment.ageingDuration }

                if (index == -1) {
                    zoneWiseData.add(ageingBucketData)
                } else {
                    zoneWiseData?.get(index!!)?.amount = zoneWiseData?.get(index!!)?.amount?.plus(payment.amount)
                }
            } else {
                arrayListAgeingBucketZone.add(ageingBucketData)
                data[payment.zone] = arrayListAgeingBucketZone
            }
        }
        return data
    }

    override suspend fun getOrgCollection(request: OrganizationReceivablesRequest): List<OutstandingResponse> {
        val startDate = YearMonth.of(request.year, request.month).minusMonths(request.count.toLong()).atDay(1).atStartOfDay()
        val endDate = YearMonth.of(request.year, request.month).plusMonths(1).atDay(1).atStartOfDay()
        val documents = OpenSearchClient().getOrgCollection(request, startDate, endDate)
        val monthList = mutableListOf<String>()
        val monthStart = YearMonth.of(request.year, request.month).minusMonths(request.count.toLong())
        for (i in 1..request.count) {
            monthList.add(monthStart.plusMonths(i.toLong()).toString())
        }
        val output = documents?.groupBy { SimpleDateFormat("yyyy-MM").format(it?.transactionDate) }!!.mapValues { it.value.sumOf { ((it?.amountLoc ?: 0.toBigDecimal()) - (it?.payLoc ?: 0.toBigDecimal())) * it?.signFlag.toString().toBigDecimal() } }
        val outstandingResponse = monthList.map { OutstandingResponse(it, 0.toBigDecimal(), "INR") }
        for (res in outstandingResponse) {
            output.forEach {
                if (it.key.uppercase() == res.duration) {
                    res.amount = it.value
                }
            }
        }
        outstandingResponse.forEach { it.duration = Month.of(it.duration!!.split("-")[1].toInt()).toString() + "-" + it.duration!!.split("-")[0] }
        return outstandingResponse
    }

    override suspend fun getOrgPayables(request: OrgPayableRequest): OrgPayableResponse {
        val data = OpenSearchClient().getOrgPayables(orgId = request.orgId)
        val ledgerAmount = data?.aggregations()?.get("ledgerAmount")?.sum()?.value()
        val currencyBreakUp = getCurrencyBucket(data)
        val collectionData = getOrgCollection(OrganizationReceivablesRequest(orgId = request.orgId))
        val ageingBucket = mutableListOf<PayableAgeingBucket>()
        listOf("Not Due", "0-30", "31-60", "61-90", "91-180", "181-365", "365+").forEach {
            ageingBucket.add(getAgeingData(request.orgId, it))
        }
        val totalReceivable = PayableOutstandingResponse(currency = "INR", amount = ledgerAmount?.toBigDecimal(), breakup = currencyBreakUp)
        return OrgPayableResponse(
            totalReceivables = totalReceivable,
            collectionTrend = collectionData,
            ageingBucket = ageingBucket
        )
    }

    private fun getAgeingData(orgId: String?, age: String): PayableAgeingBucket {
        val dateList = getDateFromAge(age)
        val data = OpenSearchClient().getOrgPayables(orgId, dateList[0], dateList[1])
        return PayableAgeingBucket(age, getLedgerAmount(data), getCurrencyBucket(data))
    }

    private fun getDateFromAge(age: String): List<Timestamp?> {
        val today = LocalDate.now().atStartOfDay()
        return when (age) {
            "Not Due" -> { listOf(Timestamp.valueOf(today), null) }
            "0-30" -> { listOf(Timestamp.valueOf(today.minusDays(30)), Timestamp.valueOf(today)) }
            "31-60" -> { listOf(Timestamp.valueOf(today.minusDays(60)), Timestamp.valueOf(today.minusDays(30))) }
            "61-90" -> { listOf(Timestamp.valueOf(today.minusDays(90)), Timestamp.valueOf(today.minusDays(60))) }
            "91-180" -> { listOf(Timestamp.valueOf(today.minusDays(180)), Timestamp.valueOf(today.minusDays(90))) }
            "181-365" -> { listOf(Timestamp.valueOf(today.minusDays(365)), Timestamp.valueOf(today.minusDays(180))) }
            "365+" -> { listOf(null, Timestamp.valueOf(today.minusDays(365))) }
            else -> { listOf(null, null) }
        }
    }

    private fun getCurrencyBucket(data: SearchResponse<Void>?): List<DueAmount>? {
        return data?.aggregations()?.get("currency")?.sterms()?.buckets()?.array()?.map {
            DueAmount(
                currency = it.key(),
                amount = it.aggregations()["currAmount"]?.sum()?.value()?.toBigDecimal(),
                invoicesCount = it.docCount().toInt()
            )
        }
    }

    private fun getLedgerAmount(data: SearchResponse<Void>?): BigDecimal {
        return data?.aggregations()?.get("ledgerAmount")?.sum()?.value()?.toBigDecimal() ?: 0.toBigDecimal()
    }

    override suspend fun getOverallStats(request: KamPaymentRequest): StatsForKamResponse {
        return accountUtilizationRepository.getOverallStats(request.docValue)
    }

    override suspend fun getOverallStatsForCustomers(
        request: CustomerStatsRequest
    ): ResponseList<StatsForCustomerResponse?> {
        var list = listOf<StatsForCustomerResponse?>()
        list = accountUtilizationRepository.getOverallStatsForCustomers(
            request.docValues, request.bookingPartyId,
            request.pageIndex, request.pageSize,
            request.sortType, request.sortBy
        )
        val responseList = ResponseList<StatsForCustomerResponse?>()
        responseList.list = list
        responseList.totalRecords = accountUtilizationRepository.getCount(request.docValues, request.bookingPartyId)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageSize) + 1 else 1
        responseList.pageNo = request.pageIndex
        return responseList
    }

//    private suspend fun getExchangeRateForPeriod(
//        request: List<String>,
//        dashboardCurrency: String
//    ): HashMap<String, BigDecimal> {
//        val endDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
//        val startDate =
//            LocalDateTime.now().minus(Period.ofDays(30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
//
//        val arrayListOfExchangeRateRequest: List<ExchangeRequestPeriod> = request.map { it ->
//            ExchangeRequestPeriod(
//                fromCurrency = it,
//                toCurrency = dashboardCurrency,
//                startDate,
//                endDate
//            )
//        }
//
//        var hashMapForExchangeRequest = HashMap<String, List<ExchangeRequestPeriod>>()
//        hashMapForExchangeRequest["rate_request_body"] = arrayListOfExchangeRateRequest
//
//        var response = exchangeClient.getExchangeRateForPeriod(hashMapForExchangeRequest)
//
//        var responseData = HashMap<String, BigDecimal>()
//
//        response.map {
//            responseData.put(it.fromCurrencyType, it.exchangeRate)
//        }
//        return responseData
//    }
}
