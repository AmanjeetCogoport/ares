package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ExchangeRequest
import com.cogoport.ares.api.common.models.ExchangeRequestPeriod
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.ExchangeClient
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.*
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
import com.cogoport.ares.model.payment.response.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.print.DocFlavor.STRING

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    @Inject
    lateinit var exchangeClient: ExchangeClient

    private suspend fun getExchangeRate(to:String, from: String, date: String):BigDecimal{
        val exchangeRequest = ExchangeRequest(
            from_curr = from,
            to_curr = to,
            exchange_date = date
        )
        val exchangeRate = exchangeClient.getExchangeRate(exchangeRequest)

        return exchangeRate.exchangeRate
    }

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
        var data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = OverallStatsResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        if((request?.currencyType != data?.currencyType) and (data?.currencyType!=null)){
            var exchangeRate = getExchangeRate(data?.currencyType, request?.currencyType)

            data?.totalOutstandingAmount = data?.totalOutstandingAmount?.times(exchangeRate)!!
            data?.openInvoicesAmount = data?.openInvoicesAmount?.times(exchangeRate)!!
            data?.openOnAccountPaymentAmount = data?.openOnAccountPaymentAmount?.times(exchangeRate)!!

            data?.currencyType = request?.currencyType
        }

        if(request?.serviceType?.name.equals(null)) data?.serviceType = "ALL"

        return data ?: OverallStatsResponse(id = searchKey)
    }

    private fun searchKeyOverallStats(request: OverallStatsRequest): String {
        var zoneKey: String?= null
        var serviceTypeKey: String?= null

        if (request.zone.isNullOrBlank()) zoneKey = "ALL" else  zoneKey=request?.zone?.uppercase()
        if (request?.serviceType?.name.equals(null)) serviceTypeKey = "ALL" else  serviceTypeKey= request.serviceType.toString()

        return AresConstants.OVERALL_STATS_PREFIX + zoneKey+ AresConstants.KEY_DELIMITER + serviceTypeKey
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        validateInput(request.zone, request.role)
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(request.zone, request.serviceType)
        var data = mutableListOf<OverallAgeingStatsResponse>()
        outstandingResponse.map {
            if(it.currency!=request.currencyType){
                var exchangeRate = getExchangeRate(it?.currency, request?.currencyType)
                it.amount = it.amount.times(exchangeRate)
                it.currency = request?.currencyType!!
            }
            data.add(overallAgeingConverter.convertToModel(it))
        }
        val durationKey = listOf("1-30", "31-60", "61-90", ">90", "Not Due")
        val key = data.map { it.ageingDuration }
        durationKey.forEach {
            if (!key.contains(it)) {
                data.add(
                    OverallAgeingStatsResponse(it, 0.toBigDecimal(), "INR", request?.serviceType?.name, request?.currencyType)
                )
            }
        }
        data = data.sortedBy { it.ageingDuration }.toMutableList()
        data.add(0, data.removeAt(4))
        return data
    }

    override suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse {
        validateInput(request.zone, request.role, request.quarterYear.split("_")[0][1].toString().toInt(), request.quarterYear.split("_")[1].toInt())
        val searchKey = searchKeyCollectionTrend(request)
        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = CollectionResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if (data != null){
            if(data.currencyType != request.currencyType){
                var exchangeRate = getExchangeRate(data?.currencyType, request.currencyType)

                data?.totalReceivableAmount = data?.totalReceivableAmount?.times(exchangeRate)!!
                data?.totalCollectedAmount = data?.totalCollectedAmount?.times(exchangeRate)!!
                data?.currencyType = request.currencyType

                data?.trend?.forEach {
                    if(it?.currencyType != request.currencyType){
                        it?.collectableAmount = it?.collectableAmount?.times(exchangeRate)
                        it?.receivableAmount = it?.receivableAmount?.times(exchangeRate)
                        it?.currencyType = request.currencyType
                    }
                }
            }
        }

        return data ?: CollectionResponse(id = searchKey)

    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        var zoneKey: String?= null
        var serviceTypeKey: String?= null

        if (request.zone.isNullOrBlank()) zoneKey = "ALL" else  zoneKey=request?.zone?.uppercase()

        if (request?.serviceType?.name.equals(null)) serviceTypeKey = "ALL" else  serviceTypeKey= request.serviceType.toString()

        return AresConstants.COLLECTIONS_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0]

    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding {
        validateInput(request.zone, request.role)

        var zoneKey: String?= null
        var serviceTypeKey: String?= null

        if (request.zone.isNullOrBlank()) zoneKey = "ALL" else  zoneKey=request?.zone?.uppercase()

        if (request?.serviceType?.name.equals(null)) serviceTypeKey = "ALL" else  serviceTypeKey= request.serviceType.toString()

        val searchKey = AresConstants.MONTHLY_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey

        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )

        if(data != null){
            data?.list?.forEach {
                if((it.currencyType != request.currencyType) && (it.currencyType != null)){
                    var exchangeRate = getExchangeRate(it.currencyType, request.currencyType)
                    it?.amount?.times(exchangeRate)
                    it?.currencyType = request?.currencyType
                }

            }
        }

        return data ?: MonthlyOutstanding(id = searchKey)
    }

    override suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding {
        validateInput(request.zone, request.role)

        var zoneKey: String?= null
        var serviceTypeKey: String?= null


        if (request.zone.isNullOrBlank()) zoneKey = "ALL" else  zoneKey=request?.zone?.uppercase()

        if (request?.serviceType?.name.equals(null)) serviceTypeKey = "ALL" else  serviceTypeKey= request.serviceType.toString()

        val searchKey = AresConstants.QUARTERLY_TREND_PREFIX + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey

        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index =
            AresConstants.SALES_DASHBOARD_INDEX
        )

        if(data != null){
            data?.list?.forEach {
                if((it.currencyType != request.currencyType) && (it.currencyType != null)){
                    var exchangeRate = getExchangeRate(it.currencyType, request.currencyType)
                    it?.amount?.times(exchangeRate)
                    it?.currencyType = request?.currencyType
                }

            }
        }
        return data ?: QuarterlyOutstanding(id = searchKey)
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        validateInput(request.zone, request.role)
        val dsoList = mutableListOf<DsoResponse>()
        val dpoList = mutableListOf<DpoResponse>()
        var currencyType: String? = null
        val sortQuarterList = request.quarterYear.sortedBy { it.split("_")[1] + it.split("_")[0][1] }
        for (q in sortQuarterList) {
            val salesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_SALES_OUTSTANDING_PREFIX, request.serviceType)
            val salesResponse = clientResponse(salesResponseKey)
            val dso = mutableListOf<DsoResponse>()
            for (hts in salesResponse?.hits()?.hits()!!) {
                val data = hts.source()
                dso.add(DsoResponse(data!!.month.toString(), data.value, data.currencyType))
            }
            val monthListDso = dso.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDso.contains(it)) {
                    dso.add(DsoResponse(it, 0.toBigDecimal(), null))
                }
            }
            dso.sortedBy { it.month }.forEach { dsoList.add(it) }

            val payablesResponseKey = searchKeyDailyOutstanding(request.zone, q.split("_")[0][1].toString().toInt(), q.split("_")[1].toInt(), AresConstants.DAILY_PAYABLES_OUTSTANDING_PREFIX, request.serviceType)
            val payablesResponse = clientResponse(payablesResponseKey)
            val dpo = mutableListOf<DpoResponse>()
            for (hts in payablesResponse?.hits()?.hits()!!) {
                val data = hts.source()
                dpo.add(DpoResponse(data!!.month.toString(), data.value, data.currencyType))
            }
            val monthListDpo = dpo.map { it.month }
            getMonthFromQuarter(q.split("_")[0][1].toString().toInt()).forEach {
                if (!monthListDpo.contains(it)) {
                    dpo.add(DpoResponse(it, 0.toBigDecimal(), null))
                }
            }
            dpo.sortedBy { it.month }.forEach { dpoList.add(it) }
        }

        val currentKey = searchKeyDailyOutstanding(request.zone, AresConstants.CURR_QUARTER, AresConstants.CURR_YEAR, AresConstants.DAILY_SALES_OUTSTANDING_PREFIX, request.serviceType)
        val currResponse = clientResponse(currentKey)
        var averageDso = 0.toFloat()

        var currentDso = 0.toFloat()
        for (hts in currResponse?.hits()?.hits()!!) {
            val data = hts.source()
            averageDso += data!!.value.toFloat()
            if (data.month == AresConstants.CURR_MONTH) {
                currentDso = hts.source()!!.value.toFloat()
                currencyType =hts.source()!!.currencyType
            }
        }

        var dsoResponseData = dsoList.map {
            DsoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dsoForTheMonth, it.currencyType)
        }

        var dpoResponseData = dpoList.map {
            DpoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dpoForTheMonth, it.currencyType)
        }

        var avgDsoAmount = (averageDso / 3).toBigDecimal()

        if((request.currencyType != currencyType) && (currencyType != null)){
            var exchangeRate = getExchangeRate(currencyType, request.currencyType)
            currentDso.toBigDecimal().times(exchangeRate)
            avgDsoAmount.times(exchangeRate)

            dsoResponseData.forEach {
                if((it.currencyType != request.currencyType) && (it.currencyType != null)){
                    exchangeRate = getExchangeRate(it.currencyType, request.currencyType)
                    it.dsoForTheMonth.times(exchangeRate)
                    it?.currencyType = request?.currencyType
                }
            }

            dpoResponseData.forEach {
                if((it.currencyType != request.currencyType) && (it.currencyType != null)){
                    exchangeRate = getExchangeRate(it.currencyType, request.currencyType)
                    it.dpoForTheMonth.times(exchangeRate)
                    it?.currencyType = request?.currencyType
                }
            }
        }
        return DailySalesOutstanding(currentDso.toBigDecimal()
            , avgDsoAmount, dsoResponseData, dpoResponseData, request.serviceType?.name, request.currencyType)
    }

    private fun clientResponse(key: List<String>): SearchResponse<DailyOutstandingResponse>? {
        return OpenSearchClient().listApi(
            index = AresConstants.SALES_DASHBOARD_INDEX,
            classType = DailyOutstandingResponse::class.java,
            values = key
        )
    }

    private fun searchKeyDailyOutstanding(zone: String?, quarter: Int, year: Int, index: String, serviceType: ServiceType?): MutableList<String> {
        return generateKeyByMonth(getMonthFromQuarter(quarter), zone, year, index,serviceType)
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

    private fun generateKeyByMonth(monthList: List<String>, zone: String?, year: Int, index: String, serviceType: ServiceType?): MutableList<String> {
        val keyList = mutableListOf<String>()
        for (item in monthList) {

            var zoneKey: String?= null
            var serviceTypeKey: String?= null

            if (zone.isNullOrBlank()) zoneKey = "ALL" else  zoneKey= zone?.uppercase()
            if (serviceType?.name.equals(null)) serviceTypeKey = "ALL" else  serviceTypeKey= serviceType.toString()

            keyList.add(index + zoneKey + AresConstants.KEY_DELIMITER + serviceTypeKey + AresConstants.KEY_DELIMITER +  item + AresConstants.KEY_DELIMITER + year)
        }
        return keyList
    }

    override suspend fun getReceivableByAge(request: ReceivableRequest): ReceivableAgeingResponse {
        val payment = accountUtilizationRepository.getReceivableByAge(request.zone)
        if (payment.size == 0) {
            return ReceivableAgeingResponse(listOf(request.zone))
        }
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
        val outstandingResponse = monthList.map { OutstandingResponse(it, 0.toBigDecimal()) }
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
                amount = it.aggregations().get("currAmount")?.sum()?.value()?.toBigDecimal(),
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

    suspend fun getExchangeRate(from_currency: String?, to_currency: String?): BigDecimal{
        val end_date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
        val start_date = LocalDateTime.now().minus(Period.ofDays( 30 )).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()

        val exchangeRateRequest = ExchangeRequestPeriod(
            from_currency,
            to_currency,
            start_date,
            end_date
        )

        var response = exchangeClient.getExchangeRateForPeriod(exchangeRateRequest)

        return response.exchangeRate
    }

}
