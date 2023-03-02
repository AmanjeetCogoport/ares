package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.InvoiceTimeLineResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.model.common.AresModelConstants
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
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.CollectionTrendResponse
import com.cogoport.ares.model.payment.response.DailyOutstandingResponse
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsResponseData
import com.cogoport.ares.model.payment.response.PayableOutstandingResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.brahma.opensearch.Client
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    @Inject
    lateinit var openSearchService: OpenSearchService

    @Inject
    lateinit var exchangeRateHelper: ExchangeRateHelper

    @Inject
    lateinit var businessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

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

    private suspend fun getDefaultersOrgIds(): List<UUID>? {
        return businessPartnersServiceImpl.listTradePartyDetailIds()
    }

    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }

    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }

    override suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponseData {
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val quarter: Int = AresConstants.CURR_QUARTER
        val year: Int = AresConstants.CURR_YEAR

        validateInput(zone, request.role)

        val searchKey = searchKeyOverallStats(request)

        val defaultersOrgIds = getDefaultersOrgIds()

        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = OverallStatsResponse::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data?.list.isNullOrEmpty()) {
            openSearchService.generateOverallStats(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = OverallStatsResponse::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val uniqueCurrencyList = data?.list?.map { it.dashboardCurrency }?.distinct()!!
        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)

        val formattedData = OverallStatsResponseData(dashboardCurrency = request.dashboardCurrency!!)

        data.list?.map {
            val avgExchangeRate = exchangeRate[it.dashboardCurrency]
            formattedData.totalOutstandingAmount = formattedData.totalOutstandingAmount.plus(it.totalOutstandingAmount.times(avgExchangeRate!!)).setScale(4, RoundingMode.UP)
            formattedData.openInvoicesAmount = formattedData.openInvoicesAmount.plus(it.openInvoicesAmount.times(avgExchangeRate)).setScale(4, RoundingMode.UP)
            formattedData.openOnAccountPaymentAmount = formattedData.openOnAccountPaymentAmount.plus(it.openOnAccountPaymentAmount.times(avgExchangeRate)).setScale(4, RoundingMode.UP)
            formattedData.dashboardCurrency = request.dashboardCurrency!!
            formattedData.openInvoicesCount = formattedData.openInvoicesCount.plus(it.openInvoicesCount)
        }
        formattedData.organizationCount = accountUtilizationRepository.getOrganizationCountForOverallStats(zone, serviceType, invoiceCurrency, defaultersOrgIds)
        return formattedData
    }

    private fun searchKeyOverallStats(request: OverallStatsRequest): String {
        val keyMap = generatingOpenSearchKey(request.zone, request.serviceType, request.invoiceCurrency)
        return AresConstants.OVERALL_STATS_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]
    }

    private fun generatingOpenSearchKey(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?): Map<String, String?> {
        val zoneKey = if (zone.isNullOrBlank()) "ALL" else zone.uppercase()
        val serviceTypeKey = if (serviceType?.name.equals(null)) "ALL" else serviceType.toString()
        val invoiceCurrencyKey = if (invoiceCurrency.isNullOrBlank()) "ALL" else invoiceCurrency.uppercase()
        return mapOf("zoneKey" to zoneKey, "serviceTypeKey" to serviceTypeKey, "invoiceCurrencyKey" to invoiceCurrencyKey)
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        val defaultersOrgIds = getDefaultersOrgIds()
        val outstandingResponse = unifiedDBRepo.getOutstandingByAge(request.serviceType?.name?.lowercase(), defaultersOrgIds, request.companyType, request.cogoEntityCode)

        val durationKey = listOf("1-30", "31-60", "61-90", "91-180", "181-365", ">365", "Not Due")

        if (outstandingResponse.isEmpty()) {
            return durationKey.map {
                OverallAgeingStatsResponse(
                    ageingDuration = it,
                    amount = 0.toBigDecimal(),
                    dashboardCurrency = "INR"
                )
            }
        }

        val data = mutableListOf<OverallAgeingStatsResponse>()
        var formattedData = mutableListOf<OverallAgeingStatsResponse>()

        val uniqueCurrencyList: List<String> = outstandingResponse.map { it.dashboardCurrency!! }

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, "INR")

        outstandingResponse.map { response ->
            if (response.dashboardCurrency != "INR") {
                val avgExchangeRate = exchangeRate[response.dashboardCurrency]
                response.amount = response.amount.times(avgExchangeRate!!).setScale(4, RoundingMode.UP)
                response.dashboardCurrency = "INR"
            }
            data.add(overallAgeingConverter.convertToModel(response))
        }

        data.map { item ->
            val index = formattedData.indexOfFirst { (it.ageingDuration?.equals(item.ageingDuration))!! }
            if (index == -1) {
                formattedData.add(item)
            } else {
                formattedData[index].amount == formattedData[index].amount?.plus(item.amount!!)
            }
        }

        val key = formattedData.map { it.ageingDuration }
        durationKey.forEach {
            if (!key.contains(it)) {
                formattedData.add(
                    OverallAgeingStatsResponse(
                        it,
                        0.toBigDecimal(),
                        "INR"
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

        val defaultersOrgIds = getDefaultersOrgIds()
        validateInput(zone, request.role, quarter, year)
        val searchKey = searchKeyCollectionTrend(request)
        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = CollectionResponse::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        if (data == null) {
            openSearchService.generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = CollectionResponse::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val requestExchangeRate: List<String> = data?.trend?.map { it.dashboardCurrency }?.distinct()!!
        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(requestExchangeRate, request.dashboardCurrency)
        val avgExchangeRate = exchangeRate[data.dashboardCurrency]

        data.totalReceivableAmount = data.totalReceivableAmount?.times(avgExchangeRate!!)
        data.totalCollectedAmount = data.totalCollectedAmount?.times(avgExchangeRate!!)
        data.dashboardCurrency = request.dashboardCurrency

        data.trend?.forEach {
            val avgTrendExchangeRate = exchangeRate[it.dashboardCurrency]
            it.collectableAmount = it.collectableAmount.times(avgTrendExchangeRate!!)
            it.receivableAmount = it.receivableAmount.times(avgTrendExchangeRate)
            it.dashboardCurrency = request.dashboardCurrency
        }

        val formattedData = getCollectionTrendData(data)

        return CollectionResponse(
            id = searchKey,
            totalReceivableAmount = data.totalReceivableAmount?.setScale(4, RoundingMode.UP),
            totalCollectedAmount = data.totalCollectedAmount?.setScale(4, RoundingMode.UP),
            trend = formattedData,
            dashboardCurrency = request.dashboardCurrency
        )
    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        val keyMap = generatingOpenSearchKey(request.zone, request.serviceType, request.invoiceCurrency)
        return AresConstants.COLLECTIONS_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0]
    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding {
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency
        val quarter: Int = AresConstants.CURR_QUARTER
        val year: Int = AresConstants.CURR_YEAR

        validateInput(request.zone, request.role)

        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)

        val searchKey = AresConstants.MONTHLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]

        val defaultersOrgIds = getDefaultersOrgIds()
        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = MonthlyOutstanding::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data == null) {
            openSearchService.generateMonthlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = MonthlyOutstanding::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val uniqueCurrencyList: List<String> = data?.list?.map { it.dashboardCurrency }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()
        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        data.list?.forEach { outstandingRes ->
            if ((outstandingRes.dashboardCurrency != request.dashboardCurrency)) {
                val avgExchangeRate = exchangeRate[outstandingRes.dashboardCurrency]
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        val newData = getMonthlyOutStandingData(data)

        return MonthlyOutstanding(
            list = newData,
            id = searchKey
        )
    }

    private fun getMonthlyOutStandingData(data: MonthlyOutstanding?): List<OutstandingResponse>? {
        val listOfOutStanding: List<OutstandingResponse>? = data?.list?.groupBy { it.duration }?.values?.map { it ->
            return@map OutstandingResponse(
                amount = it.sumOf { it.amount }.setScale(4, RoundingMode.UP),
                duration = it.first().duration,
                dashboardCurrency = it.first().dashboardCurrency,
            )
        }

        return listOfOutStanding
    }

    private fun getCollectionTrendData(data: CollectionResponse?): List<CollectionTrendResponse>? {
        val listOfCollectionTrend: List<CollectionTrendResponse>? = data?.trend?.groupBy { it.duration }?.values?.map { it ->
            return@map CollectionTrendResponse(
                receivableAmount = it.sumOf { it.receivableAmount }.setScale(4, RoundingMode.UP),
                collectableAmount = it.sumOf { it.collectableAmount }.setScale(4, RoundingMode.UP),
                duration = it.first().duration,
                dashboardCurrency = it.first().dashboardCurrency,
            )
        }

        return listOfCollectionTrend
    }

    private fun getQuarterlyOutStandingData(data: QuarterlyOutstanding?): List<OutstandingResponse>? {
        val listOfOutStanding: List<OutstandingResponse>? = data?.list?.groupBy { it.duration }?.values?.map {
            return@map OutstandingResponse(
                amount = it.sumOf { it.amount }.setScale(4, RoundingMode.UP),
                duration = it.first().duration,
                dashboardCurrency = it.first().dashboardCurrency
            )
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

        val defaultersOrgIds = getDefaultersOrgIds()

        val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)

        val searchKey =
            AresConstants.QUARTERLY_TREND_PREFIX + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"]

        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = QuarterlyOutstanding::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data == null) {
            openSearchService.generateQuarterlyOutstanding(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)
            data = OpenSearchClient().search(
                searchKey = searchKey,
                classType = QuarterlyOutstanding::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        val uniqueCurrencyList: List<String> = data?.list?.map { it.dashboardCurrency }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()
        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        data.list?.forEach { outstandingRes ->
            if (outstandingRes.dashboardCurrency != request.dashboardCurrency) {
                val avgExchangeRate = exchangeRate[outstandingRes.dashboardCurrency]
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        val newData = getQuarterlyOutStandingData(data)

        return QuarterlyOutstanding(
            list = newData,
            id = searchKey
        )
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        validateInput(request.zone, request.role)
        val dsoList = mutableListOf<DsoResponse>()
        val defaultersOrgIds = getDefaultersOrgIds()

        val sortQuarterList = request.quarterYear.sortedBy { it.split("_")[1] + it.split("_")[0][1] }
        for (q in sortQuarterList) {
            val salesResponseKey = searchKeyDailyOutstanding(
                request.zone,
                q.split("_")[0][1].toString().toInt(),
                q.split("_")[1].toInt(),
                AresConstants.DAILY_SALES_OUTSTANDING_PREFIX,
                request.serviceType,
                request.invoiceCurrency
            )
            var salesResponse = clientResponse(salesResponseKey)

            val quarter = q.split("_")[0][1].toString().toInt()
            val year = q.split("_")[1].toInt()
            val monthList = getMonthFromQuarter(quarter)

            if (salesResponse?.hits()?.hits().isNullOrEmpty()) {
                monthList.forEach {
                    val date = "$year-$it-01".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    openSearchService.generateDailySalesOutstanding(
                        request.zone,
                        q.split("_")[0][1].toString().toInt(),
                        q.split("_")[1].toInt(),
                        request.serviceType,
                        request.invoiceCurrency,
                        date,
                        request.dashboardCurrency,
                        defaultersOrgIds
                    )
                }
                salesResponse = clientResponse(salesResponseKey)
            }

            val dso = mutableListOf<DsoResponse>()
            for (hts in salesResponse?.hits()?.hits()!!) {
                val data = hts.source()
                val dsoResponse = DsoResponse(month = "", dsoForTheMonth = 0.toBigDecimal())
                val uniqueCurrencyListSize = (hts.source()?.list?.map { it.dashboardCurrency!! })?.size
                data?.list?.map {
                    dsoResponse.month = it.month.toString()
                    dsoResponse.dsoForTheMonth = dsoResponse.dsoForTheMonth.plus(it.value)
                }
                dsoResponse.dsoForTheMonth = dsoResponse.dsoForTheMonth.div(uniqueCurrencyListSize?.toBigDecimal()!!)
                dso.add(dsoResponse)
            }
        }

        val dsoResponseData = dsoList.map {
            DsoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dsoForTheMonth)
        }

        return DailySalesOutstanding(dsoResponse = dsoResponseData)
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
            1 -> { listOf("01", "02", "03") }
            2 -> { listOf("04", "05", "06") }
            3 -> { listOf("07", "08", "09") }
            4 -> { listOf("10", "11", "12") }
            else -> { throw AresException(AresError.ERR_1004, "") }
        }
    }

    private fun generateKeyByMonth(monthList: List<String>, zone: String?, year: Int, index: String, serviceType: ServiceType?, invoiceCurrency: String?): MutableList<String> {
        val keyList = mutableListOf<String>()
        for (item in monthList) {
            val keyMap = generatingOpenSearchKey(zone, serviceType, invoiceCurrency)
            keyList.add(index + keyMap["zoneKey"] + AresConstants.KEY_DELIMITER + keyMap["serviceTypeKey"] + AresConstants.KEY_DELIMITER + keyMap["invoiceCurrencyKey"] + AresConstants.KEY_DELIMITER + item + AresConstants.KEY_DELIMITER + year)
        }
        return keyList
    }

    override suspend fun getReceivableByAge(request: ReceivableRequest): HashMap<String, ArrayList<AgeingBucketZone>> {
        val serviceType: ServiceType? = request.serviceType
        val invoiceCurrency: String? = request.invoiceCurrency
        val defaultersOrgIds = getDefaultersOrgIds()
        val payments = accountUtilizationRepository.getReceivableByAge(request.zone, serviceType, invoiceCurrency, defaultersOrgIds)
        val data = HashMap<String, ArrayList<AgeingBucketZone>>()
        val zoneList = listOf<String>("NORTH", "EAST", "WEST", "SOUTH")
        val ageingDurationList = listOf<String>("1-30", "31-60", "61-90", ">90", "91-180", "181-365", "365+", "Not Due")

        if (payments.isEmpty()) {
            zoneList.forEach {
                val listData = ageingDurationList.map { duration ->
                    AgeingBucketZone(
                        ageingDuration = duration,
                        amount = 0.toBigDecimal()
                    )
                }
                data[it] = listData as ArrayList<AgeingBucketZone>
            }
            return data
        }
        val uniqueCurrencyList: List<String> = payments.map { it.dashboardCurrency!! }.distinct()

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)

        payments.forEach { payment ->
            val zone = payment.zone
            val arrayListAgeingBucketZone = ArrayList<AgeingBucketZone>()

            if (payment.dashboardCurrency != request.dashboardCurrency) {
                val avgExchangeRate = exchangeRate[payment.dashboardCurrency]
                payment.amount = payment.amount.times(avgExchangeRate!!)
                payment.dashboardCurrency = request.dashboardCurrency
            }

            val ageingBucketData = AgeingBucketZone(
                ageingDuration = payment.ageingDuration,
                amount = payment.amount.setScale(4, RoundingMode.UP),
                dashboardCurrency = payment.dashboardCurrency
            )

            if (data.keys.contains(zone)) {
                val zoneWiseData = data[zone]

                val index = zoneWiseData?.indexOfFirst { it.ageingDuration == payment.ageingDuration }

                if (index == -1) {
                    zoneWiseData.add(ageingBucketData)
                } else {
                    zoneWiseData?.get(index!!)?.amount = zoneWiseData?.get(index!!)?.amount?.plus(payment.amount)?.setScale(4, RoundingMode.UP)
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

    override suspend fun getStatsForTradeParties(request: TradePartyStatsRequest): ResponseList<OverallStatsForTradeParty?> {
        val list = accountUtilizationRepository.getOverallStatsForTradeParty(
            request.docValues, request.pageIndex, request.pageSize
        )
        val responseList = ResponseList<OverallStatsForTradeParty?>()
        responseList.list = list
        responseList.totalRecords = accountUtilizationRepository.getTradePartyCount(request.docValues)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageSize) + 1 else 1
        responseList.pageNo = request.pageIndex
        return responseList
    }

    override suspend fun getInvoiceListForTradeParties(request: InvoiceListRequestForTradeParty): ResponseList<InvoiceListResponse?> {
        val invoiceList = accountUtilizationRepository.getInvoiceListForTradeParty(
            request.docValues, request.sortBy, request.sortType,
            request.pageIndex, request.pageSize
        )
        val responseList = ResponseList<InvoiceListResponse?>()
        responseList.list = invoiceList
        responseList.totalRecords = accountUtilizationRepository.getInvoicesCountForTradeParty(request.docValues)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageSize) + 1 else 1
        responseList.pageNo = request.pageIndex
        return responseList
    }

    override suspend fun getSalesFunnel(month: String?): SalesFunnelResponse? {
        val year = AresModelConstants.CURR_YEAR
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        val monthKey = when (!month.isNullOrEmpty()) {
            true -> months.indexOf(month) + 1
            else -> AresModelConstants.CURR_MONTH
        }

        val searchKey = AresConstants.SALES_FUNNEL_PREFIX + months[monthKey - 1] + AresConstants.KEY_DELIMITER + year

        var openSearchData = OpenSearchClient().search(
            searchKey = searchKey,
            classType = SalesFunnelResponse::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (openSearchData == null) {
            openSearchService.generatingSalesFunnelData(monthKey, year, searchKey)
            openSearchData = OpenSearchClient().search(
                searchKey = searchKey,
                classType = SalesFunnelResponse::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }

        return openSearchData
    }

    override suspend fun getInvoiceTimeline(startDate: String?, endDate: String?): InvoiceTimeLineResponse? {
        val updatedStartDate = when (!startDate.isNullOrEmpty()) {
            true -> startDate
            else -> "${AresConstants.CURR_YEAR}-${generateMonthKeyIndex(AresConstants.CURR_MONTH)}-01"
        }.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val updatedEndDate = when (!endDate.isNullOrEmpty()) {
            true -> endDate
            else -> "${AresConstants.CURR_YEAR}-${generateMonthKeyIndex(AresConstants.CURR_MONTH)}-${LocalDate.parse(updatedStartDate).month.length(LocalDate.parse(updatedStartDate).isLeapYear)}"
        }.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val data = unifiedDBRepo.getInvoices(updatedStartDate, updatedEndDate)

        val objectMapper = ObjectMapper()

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val mapOfData = mapOf("finance_accepted" to data.filter { it.status != "DRAFT" }, "irn_generated" to data.filter { !listOf("DRAFT", "FINANCE_ACCEPTED").contains(it.status) }, "settled" to data.filter { it.paymentStatus == "PAID" })

        val invoiceTimeLineResp = InvoiceTimeLineResponse()

        mapOfData.entries.map { (k, v) ->
            v.map { invoice ->
                val eventData = objectMapper.readValue(invoice.events, Array<InvoiceEventResponse>::class.java)
                logger().info(invoice.id.toString())
                when (k) {
                    "finance_accepted" -> {
                        val createdAtEventDate = eventData.first { it.eventName == "CREATED" }.occurredAt.time
                        var financeAcceptedEventDate = 0L
                        if (eventData.map { it.eventName }.contains("FINANCE_ACCEPTED")) {
                            financeAcceptedEventDate = eventData.first { it.eventName == "FINANCE_ACCEPTED" }.occurredAt.time
                        }
                        if (financeAcceptedEventDate != 0L) {
                            invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted = invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted?.plus(financeAcceptedEventDate.minus(createdAtEventDate))?.div(1000)?.div(60)?.div(60)
                        } else {}
                    }
                    "irn_generated" -> {
                        var financeAcceptedEventDate = 0L
                        var irnGeneratedEventDate = 0L
                        if (eventData.map { it.eventName }.contains("FINANCE_ACCEPTED")) {
                            financeAcceptedEventDate = eventData.first { it.eventName == "FINANCE_ACCEPTED" }.occurredAt.time
                        }
                        if (eventData.map { it.eventName }.contains("IRN_GENERATED")) {
                            irnGeneratedEventDate = eventData.first { it.eventName == "IRN_GENERATED" }.occurredAt.time
                        }

                        if (irnGeneratedEventDate != 0L) {
                            invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated?.plus(irnGeneratedEventDate.minus(financeAcceptedEventDate).div(1000).div(60).div(60))
                        } else {}
                    }
                    "settled" -> {
                        var irnGeneratedEventDate = 0L
                        var settlementEventDate = 0L
                        if (eventData.map { it.eventName }.contains("IRN_GENERATED")) {
                            irnGeneratedEventDate = eventData.first { it.eventName == "IRN_GENERATED" }.occurredAt.time
                        }
                        if (eventData.map { it.eventName }.contains("SETTLEMENT")) {
                            settlementEventDate =
                                eventData.first { it.eventName == "SETTLEMENT" }.occurredAt.time
                        }

                        if (settlementEventDate != 0L) {
                            invoiceTimeLineResp.tatHoursFromIrnGeneratedToSettled?.plus(
                                settlementEventDate.minus(
                                    irnGeneratedEventDate
                                ).div(1000).div(60).div(60)
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        invoiceTimeLineResp.draftInvoicesCount = data.size
        invoiceTimeLineResp.financeAcceptedInvoiceCount = mapOfData["finance_accepted"]?.size
        invoiceTimeLineResp.irnGeneratedInvoicesCount = mapOfData["irn_generated"]?.size
        invoiceTimeLineResp.settledInvoicesCount = mapOfData["settled"]?.size

        if (invoiceTimeLineResp.financeAcceptedInvoiceCount!! != 0) {
            invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted = invoiceTimeLineResp.tatHoursFromDraftToFinanceAccepted?.div(invoiceTimeLineResp.financeAcceptedInvoiceCount!!)
        }

        if (invoiceTimeLineResp.irnGeneratedInvoicesCount!! != 0) {
            invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTimeLineResp.tatHoursFromFinanceAcceptedToIrnGenerated?.div(invoiceTimeLineResp.irnGeneratedInvoicesCount!!)
        }
        if (invoiceTimeLineResp.settledInvoicesCount != 0) {
            invoiceTimeLineResp.tatHoursFromIrnGeneratedToSettled?.div(invoiceTimeLineResp.settledInvoicesCount!!)
        }

        return invoiceTimeLineResp
    }

    override suspend fun getOutstanding(date: String?): OutstandingOpensearchResponse? {
        val asOnDate = date ?: AresConstants.CURR_DATE.toLocalDate()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val searchKey = AresConstants.OUTSTANDING_PREFIX + asOnDate

        var openSearchData = OpenSearchClient().search(
            searchKey = searchKey,
            classType = OutstandingOpensearchResponse::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (openSearchData == null) {
            openSearchService.generateOutstandingData(asOnDate, searchKey)

            openSearchData = OpenSearchClient().search(
                searchKey = searchKey,
                classType = OutstandingOpensearchResponse::class.java,
                index = AresConstants.SALES_DASHBOARD_INDEX
            )
        }
        return openSearchData
    }

    override suspend fun getDailySalesStatistics(
        month: String?,
        year: Int?,
        asOnDate: String?,
        documentType: String?
    ): kotlin.collections.HashMap<String, ArrayList<Outstanding>> {
        val defaultersOrgIds = getDefaultersOrgIds()

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        var outstandingData = mutableListOf<Outstanding>()

        var hashMap = hashMapOf<String, ArrayList<Outstanding>>()

        var accTypeDocStatusMapping = mapOf(
            "SALES_INVOICE" to mapOf("accType" to "SINV", "docStatus" to listOf("FINAL", "PROFORMA")),
            "CREDIT_NOTE" to mapOf("accType" to "SCN", "docStatus" to listOf("FINAL")),
            "ON_ACCOUNT_PAYMENT" to mapOf("accType" to "REC", "docStatus" to listOf("FINAL"))
        )

        if (year != null) {
            var endDate = "$year-12-31".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            outstandingData = if (documentType in listOf("SALES_INVOICE", "CREDIT_NOTE", "ON_ACCOUNT_PAYMENT")) {
                unifiedDBRepo.generateYearlySalesOutstanding(
                    endDate,
                    accTypeDocStatusMapping[documentType]?.get("accType").toString(),
                    defaultersOrgIds,
                    accTypeDocStatusMapping[documentType]?.get("docStatus") as List<String>
                )!!
            } else {
                unifiedDBRepo.generateYearlyShipmentCreatedAt(endDate)!!
            }
        }

        if (month != null) {
            var endDate = when (year != null) {
                true -> "$year-${generateMonthKeyIndex(months.indexOf(month) + 1)}-31".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                else -> "${AresConstants.CURR_YEAR}-${generateMonthKeyIndex(months.indexOf(month) + 1)}-31".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            outstandingData = if (documentType in listOf("SALES_INVOICE", "CREDIT_NOTE", "ON_ACCOUNT_PAYMENT")) {
                unifiedDBRepo.generateMonthlyOutstanding(
                    endDate,
                    accTypeDocStatusMapping[documentType]?.get("accType").toString(),
                    defaultersOrgIds,
                    accTypeDocStatusMapping[documentType]?.get("docStatus") as List<String>
                )!!
            } else {
                unifiedDBRepo.generateMonthlyShipmentCreatedAt(endDate)!!
            }
        }

        if (asOnDate != null) {
            outstandingData = if (documentType in listOf("SALES_INVOICE", "CREDIT_NOTE", "ON_ACCOUNT_PAYMENT")) {
                unifiedDBRepo.generateDailySalesOutstanding(
                    asOnDate,
                    accTypeDocStatusMapping[documentType]?.get("accType").toString(),
                    defaultersOrgIds,
                    accTypeDocStatusMapping[documentType]?.get("docStatus") as List<String>,
                )!!
            } else {
                unifiedDBRepo.generateDailyShipmentCreatedAt(asOnDate)!!
            }
        }

        if (asOnDate == null && year == null && month == null) {
            val endDate = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            outstandingData = if (documentType in listOf("SALES_INVOICE", "CREDIT_NOTE", "ON_ACCOUNT_PAYMENT")) {
                unifiedDBRepo.generateDailySalesOutstanding(
                    endDate,
                    accTypeDocStatusMapping[documentType]?.get("accType").toString(),
                    defaultersOrgIds,
                    accTypeDocStatusMapping[documentType]?.get("docStatus") as List<String>,
                )!!
            } else {
                unifiedDBRepo.generateDailyShipmentCreatedAt(endDate)!!
            }
        }

        val uniqueCurrencyList: List<String> = outstandingData.filter { it.dashboardCurrency != null }.map { it.dashboardCurrency!! }.distinct()

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, "INR")

        outstandingData.groupBy { it -> it.duration }.entries.map { (key, value) ->
            val outStanding = Outstanding(
                amount = 0.toBigDecimal(),
                duration = key,
                dashboardCurrency = "INR",
                count = 0L
            )

            value.map { item ->
                outStanding.amount = outStanding.amount.plus(item.amount.times(exchangeRate[item.dashboardCurrency]!!))
                outStanding.count = outStanding.count?.plus(item.count!!)
            }

            if (hashMap.keys.contains(documentType)) {
                hashMap[documentType]?.add(outStanding)
            } else {
                hashMap[documentType!!] = arrayListOf(outStanding)
            }
        }
        return hashMap
    }

    override suspend fun getKamWiseOutstanding(): List<KamWiseOutstanding>? {
        var kamWiseData = unifiedDBRepo.getKamWiseOutstanding()
        return kamWiseData
    }

    private fun generateMonthKeyIndex(month: Int): String {
        return when (month < 10) {
            true -> "0$month"
            else -> month.toString()
        }
    }
}
