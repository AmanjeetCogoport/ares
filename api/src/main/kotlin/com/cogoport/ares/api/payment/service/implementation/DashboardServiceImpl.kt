package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.CollectionTrendMapper
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
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
import com.cogoport.ares.model.payment.response.DpoResponse
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.OverallStatsResponseData
import com.cogoport.ares.model.payment.response.PayableOutstandingResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.brahma.opensearch.Client
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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    @Inject
    lateinit var exchangeRateHelper: ExchangeRateHelper

    @Inject
    lateinit var businessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl

    @Inject
    lateinit var collectionTrendConverter: CollectionTrendMapper

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

        validateInput(zone, request.role)

        val defaultersOrgIds = getDefaultersOrgIds()

        val statsZoneData = accountUtilizationRepository.generateOverallStats(zone, serviceType, invoiceCurrency, defaultersOrgIds)

        val uniqueCurrencyList = statsZoneData.map { it.dashboardCurrency }.distinct()
        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)

        val formattedData = OverallStatsResponseData(dashboardCurrency = request.dashboardCurrency)

        statsZoneData.map {
            val avgExchangeRate = exchangeRate[it.dashboardCurrency]

            if (avgExchangeRate != null) {
                formattedData.totalOutstandingAmount = formattedData.totalOutstandingAmount.plus(it.totalOutstandingAmount?.times(avgExchangeRate)!!).setScale(4, RoundingMode.UP)
                formattedData.openInvoicesAmount = formattedData.openInvoicesAmount.plus(it.openInvoicesAmount?.times(avgExchangeRate)!!).setScale(4, RoundingMode.UP)
                formattedData.openOnAccountPaymentAmount = formattedData.openOnAccountPaymentAmount.plus(it.openOnAccountPaymentAmount?.times(avgExchangeRate)!!).setScale(4, RoundingMode.UP)
                formattedData.dashboardCurrency = request.dashboardCurrency
                formattedData.openInvoicesCount = formattedData.openInvoicesCount.plus(it.openInvoicesCount!!)
            }
        }
        formattedData.organizationCount = accountUtilizationRepository.getOrganizationCountForOverallStats(zone, serviceType, invoiceCurrency, defaultersOrgIds)
        return formattedData
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        validateInput(request.zone, request.role)
        val defaultersOrgIds = getDefaultersOrgIds()
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(request.zone, request.serviceType, request.invoiceCurrency, defaultersOrgIds)

        val durationKey = listOf("1-30", "31-60", "61-90", ">90", "Not Due")

        if (outstandingResponse.isEmpty()) {
            return durationKey.map {
                OverallAgeingStatsResponse(
                    ageingDuration = it,
                    amount = 0.toBigDecimal(),
                    dashboardCurrency = request.dashboardCurrency
                )
            }
        }

        val data = mutableListOf<OverallAgeingStatsResponse>()
        var formattedData = mutableListOf<OverallAgeingStatsResponse>()

        val uniqueCurrencyList: List<String> = outstandingResponse.map { it.dashboardCurrency!! }

        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency)

        outstandingResponse.map { response ->
            if (response.dashboardCurrency != request.dashboardCurrency) {
                val avgExchangeRate = exchangeRate[response.dashboardCurrency]
                response.amount = response.amount.times(avgExchangeRate!!).setScale(4, RoundingMode.UP)
                response.dashboardCurrency = request.dashboardCurrency
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

        val defaultersOrgIds = getDefaultersOrgIds()
        validateInput(zone, request.role, quarter, year)

        val collectionTrendData = accountUtilizationRepository.generateCollectionTrend(zone, quarter, year, serviceType, invoiceCurrency, defaultersOrgIds)

        val collectionTrendResponse = collectionTrendData.map {
            collectionTrendConverter.convertToModel(it)
        }

        val collectionResponse = formatCollectionTrend(collectionTrendResponse, quarter)

        val requestExchangeRate = collectionTrendResponse.map { it.dashboardCurrency }.distinct()
        val exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(requestExchangeRate, request.dashboardCurrency)

        collectionResponse.trend?.forEach {
            val avgTrendExchangeRate = exchangeRate[it.dashboardCurrency]
            if (avgTrendExchangeRate != null) {
                it.collectableAmount = it.collectableAmount.times(avgTrendExchangeRate)
                it.receivableAmount = it.receivableAmount.times(avgTrendExchangeRate)
                it.dashboardCurrency = request.dashboardCurrency
            }
        }

        val formattedData = getCollectionTrendData(collectionResponse)

        return CollectionResponse(
            id = null,
            totalReceivableAmount = collectionResponse.totalReceivableAmount?.setScale(4, RoundingMode.UP),
            totalCollectedAmount = collectionResponse.totalCollectedAmount?.setScale(4, RoundingMode.UP),
            trend = formattedData,
            dashboardCurrency = request.dashboardCurrency
        )
    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding {
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency

        validateInput(request.zone, request.role)

        val defaultersOrgIds = getDefaultersOrgIds()

        val monthlyTrendZoneData = accountUtilizationRepository.generateMonthlyOutstanding(zone, serviceType, invoiceCurrency, defaultersOrgIds)

        monthlyTrendZoneData?.forEach { it ->
            if (it.dashboardCurrency.isNullOrEmpty()) {
                it.dashboardCurrency = invoiceCurrency
            }
        }

        val uniqueCurrencyList = monthlyTrendZoneData?.map { it.dashboardCurrency!! }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()

        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        monthlyTrendZoneData.forEach { outstandingRes ->
            if ((outstandingRes.dashboardCurrency != request.dashboardCurrency)) {
                val avgExchangeRate = exchangeRate[outstandingRes.dashboardCurrency]
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        val monthlyOutstandingData = MonthlyOutstanding(
            list = monthlyTrendZoneData.map {
                OutstandingResponse(
                    amount = it.amount,
                    dashboardCurrency = it.dashboardCurrency!!,
                    duration = it.duration
                )
            }
        )

        val newData = getMonthlyOutStandingData(monthlyOutstandingData)

        return MonthlyOutstanding(
            list = newData,
            id = null
        )
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

        validateInput(zone, request.role)

        val defaultersOrgIds = getDefaultersOrgIds()

        val quarterlyTrendZoneData = accountUtilizationRepository.generateQuarterlyOutstanding(zone, serviceType, invoiceCurrency, defaultersOrgIds)
        quarterlyTrendZoneData?.forEach { it ->
            if (it.dashboardCurrency.isNullOrEmpty()) {
                it.dashboardCurrency = invoiceCurrency
            }
        }

        val uniqueCurrencyList = quarterlyTrendZoneData?.map { it.dashboardCurrency!! }?.distinct()!!

        var exchangeRate = HashMap<String, BigDecimal>()
        if (uniqueCurrencyList.isNotEmpty()) {
            exchangeRate = exchangeRateHelper.getExchangeRateForPeriod(uniqueCurrencyList, request.dashboardCurrency!!)
        }

        quarterlyTrendZoneData.forEach { outstandingRes ->
            if (outstandingRes.dashboardCurrency != request.dashboardCurrency) {
                val avgExchangeRate = exchangeRate[outstandingRes.dashboardCurrency]
                outstandingRes.amount = outstandingRes.amount.times(avgExchangeRate!!)
                outstandingRes.dashboardCurrency = request.dashboardCurrency!!
            }
        }

        val quarterlyOutstandingResponse = quarterlyTrendZoneData.map {
            OutstandingResponse(
                amount = it.amount,
                duration = it.duration,
                dashboardCurrency = it.dashboardCurrency!!
            )
        }

        val quarterlyOutstanding = QuarterlyOutstanding(
            list = quarterlyOutstandingResponse
        )

        val newData = getQuarterlyOutStandingData(quarterlyOutstanding)

        return QuarterlyOutstanding(
            list = newData,
            id = null
        )
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        validateInput(request.zone, request.role)
        val dsoList = mutableListOf<DsoResponse>()
        val dpoList = mutableListOf<DpoResponse>()
        val defaultersOrgIds = getDefaultersOrgIds()
        val zone = request.zone
        val serviceType = request.serviceType
        val invoiceCurrency = request.invoiceCurrency

        val sortQuarterList = request.quarterYear.sortedBy { it.split("_")[1] + it.split("_")[0][1] }
        for (q in sortQuarterList) {
            val quarter = q.split("_")[0][1].toString().toInt()
            val monthList = getMonthFromQuarter(quarter)
            val year = q.split("_")[1].toInt()

            val dso = mutableListOf<DsoResponse>()
            val dpo = mutableListOf<DpoResponse>()

            monthList.forEach {
                val date = "$year-$it-01".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val dailySalesZoneServiceTypeData = accountUtilizationRepository.generateDailySalesOutstanding(zone, date, serviceType, invoiceCurrency, defaultersOrgIds)

                val dsoResponse = DsoResponse(month = "", dsoForTheMonth = 0.toBigDecimal())
                var uniqueCurrencyListSize = (dailySalesZoneServiceTypeData.map { it.dashboardCurrency!! }).size

                dailySalesZoneServiceTypeData.map {
                    dsoResponse.month = it.month.toString()
                    dsoResponse.dsoForTheMonth = dsoResponse.dsoForTheMonth.plus(it.value.toBigDecimal())
                }
                dsoResponse.dsoForTheMonth = dsoResponse.dsoForTheMonth.div(uniqueCurrencyListSize.toBigDecimal())
                dso.add(dsoResponse)

                val dailyPayableZoneServiceTypeData = accountUtilizationRepository.generateDailyPayableOutstanding(zone, date, serviceType, invoiceCurrency)

                val dpoResponse = DpoResponse(month = "", dpoForTheMonth = 0.toBigDecimal())
                uniqueCurrencyListSize = (dailyPayableZoneServiceTypeData.map { it.dashboardCurrency!! }).size

                dailyPayableZoneServiceTypeData.map {
                    dpoResponse.month = it.month.toString()
                    dpoResponse.dpoForTheMonth = dpoResponse.dpoForTheMonth.plus(it.value.toBigDecimal())
                }

                dpoResponse.dpoForTheMonth = dpoResponse.dpoForTheMonth.div(uniqueCurrencyListSize.toBigDecimal())
                dpo.add(dpoResponse)
            }

            val monthListDso = dso.map {
                when (it.month.toInt() < 10) {
                    true -> "0${it.month}"
                    false -> it.month
                }
            }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDso.contains(it)) {
                    dso.add(DsoResponse(it, 0.toBigDecimal()))
                }
            }
            dso.sortedBy { it.month }.forEach { dsoList.add(it) }

            val monthListDpo = dpo.map {
                when (it.month.toInt() < 10) {
                    true -> "0${it.month}"
                    false -> it.month
                }
            }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDpo.contains(it)) {
                    dpo.add(DpoResponse(it, 0.toBigDecimal()))
                }
            }
            dpo.sortedBy { it.month }.forEach { dpoList.add(it) }
        }

        val date = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currResponse = accountUtilizationRepository.generateDailySalesOutstanding(zone, date, serviceType, invoiceCurrency, defaultersOrgIds)

        val uniqueCurrencyListSize = (currResponse.map { it.dashboardCurrency!! }).size

        var averageDso = 0.toFloat()
        var currentDso = 0.toFloat()

        currResponse.map {
            averageDso = averageDso.plus(it.value.toFloat())
            if (it.month == AresConstants.CURR_MONTH) {
                currentDso = currentDso.plus(it.value.toFloat())
            }
        }
        averageDso = averageDso.toBigDecimal().div(uniqueCurrencyListSize.toBigDecimal()).toFloat()
        currentDso = currentDso.toBigDecimal().div(uniqueCurrencyListSize.toBigDecimal()).toFloat()

        val dsoResponseData = dsoList.map {
            DsoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dsoForTheMonth)
        }

        val dpoResponseData = dpoList.map {
            DpoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dpoForTheMonth)
        }

        val avgDsoAmount = averageDso.div(3.toFloat()).toBigDecimal()

        return DailySalesOutstanding(currentDso.toBigDecimal(), avgDsoAmount, dsoResponseData, dpoResponseData, request.serviceType?.name, request.dashboardCurrency)
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

    private fun getMonthFromQuarterV2(quarter: Int): List<String> {
        return when (quarter) {
            1 -> { listOf("January", "February", "March") }
            2 -> { listOf("April", "May", "June") }
            3 -> { listOf("July", "August", "September") }
            4 -> { listOf("October", "November", "December") }
            else -> { throw AresException(AresError.ERR_1004, "") }
        }
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

    private fun formatCollectionTrend(data: List<CollectionTrendResponse>, quarter: Int): CollectionResponse {
        val trendData = mutableListOf<CollectionTrendResponse>()
        var totalAmount: BigDecimal? = null
        var totalCollected: BigDecimal? = null
        var dashboardCurrency: String? = null

        val monthList = mutableListOf<String?>()
        for (row in data) {
            if (row.duration != "Total") {
                trendData.add(CollectionTrendResponse(row.duration, row.receivableAmount, row.collectableAmount, row.dashboardCurrency))
                monthList.add(row.duration)
                dashboardCurrency = row.dashboardCurrency
            } else {
                totalAmount = row.receivableAmount
                totalCollected = row.collectableAmount
                dashboardCurrency = row.dashboardCurrency
            }
        }
        getMonthFromQuarterV2(quarter).forEach {
            if (!monthList.contains(it)) {
                trendData.add(CollectionTrendResponse(it, 0.toBigDecimal(), 0.toBigDecimal(), "INR"))
            }
        }
        return CollectionResponse(totalAmount, totalCollected, trendData.sortedBy { Month.valueOf(it.duration!!.uppercase()) }, null, dashboardCurrency!!)
    }
}
