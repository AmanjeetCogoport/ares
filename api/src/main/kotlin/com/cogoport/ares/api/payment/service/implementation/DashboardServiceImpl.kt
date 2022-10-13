package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
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
import com.cogoport.ares.model.payment.ReceivableByAgeViaServiceType
import com.cogoport.ares.model.payment.ReceivableByAgeViaZone
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
import com.cogoport.ares.model.payment.response.ReceivableAgeingResponse
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
import java.time.Month
import java.time.YearMonth

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
        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = OverallStatsResponse ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        return data ?: OverallStatsResponse(id = searchKey)
    }

    private fun searchKeyOverallStats(request: OverallStatsRequest): String {
        return if (request.zone.isNullOrBlank()) AresConstants.OVERALL_STATS_PREFIX + "ALL" else AresConstants.OVERALL_STATS_PREFIX + request.zone
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse> {
        validateInput(request.zone, request.role)
        val outstandingResponse = accountUtilizationRepository.getAgeingBucket(request.zone)
        var data = mutableListOf<OverallAgeingStatsResponse>()
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
        return data ?: CollectionResponse(id = searchKey)
    }

    private fun searchKeyCollectionTrend(request: CollectionRequest): String {
        return if (request.zone.isNullOrBlank()) AresConstants.COLLECTIONS_TREND_PREFIX + "ALL" + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0] else AresConstants.COLLECTIONS_TREND_PREFIX + request.zone + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[1] + AresConstants.KEY_DELIMITER + request.quarterYear.split("_")[0]
    }

    override suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding {
        validateInput(request.zone, request.role)
        val searchKey = if (request.zone.isNullOrBlank()) AresConstants.MONTHLY_TREND_PREFIX + "ALL" else AresConstants.MONTHLY_TREND_PREFIX + request.zone
        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = MonthlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        return data ?: MonthlyOutstanding(id = searchKey)
    }

    override suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding {
        validateInput(request.zone, request.role)
        val searchKey = if (request.zone.isNullOrBlank()) AresConstants.QUARTERLY_TREND_PREFIX + "ALL" else AresConstants.QUARTERLY_TREND_PREFIX + request.zone
        val data = OpenSearchClient().search(
            searchKey = searchKey,
            classType = QuarterlyOutstanding ::class.java,
            index = AresConstants.SALES_DASHBOARD_INDEX
        )
        return data ?: QuarterlyOutstanding(id = searchKey)
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
                    dso.add(DsoResponse(it, 0.toBigDecimal()))
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
                    dpo.add(DpoResponse(it, 0.toBigDecimal()))
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
            averageDso += data!!.value.toFloat()
            if (data.month == AresConstants.CURR_MONTH) {
                currentDso = hts.source()!!.value.toFloat()
            }
        }
        return DailySalesOutstanding(currentDso.toBigDecimal(), (averageDso / 3).toBigDecimal(), dsoList.map { DsoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dsoForTheMonth) }, dpoList.map { DpoResponse(Month.of(it.month.toInt()).toString().slice(0..2), it.dpoForTheMonth) })
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
        val serviceType: ServiceType? = request.serviceType
        val currencyType: String? = request.currencyType
        val payment = accountUtilizationRepository.getReceivableByAge(request.zone, serviceType, currencyType)
        if (payment.size == 0) {
            return ReceivableAgeingResponse(listOf(request.zone), listOf(serviceType))
        }
        val receivableNorthBucket = mutableListOf<AgeingBucketZone>()
        val receivableSouthBucket = mutableListOf<AgeingBucketZone>()
        val receivableEastBucket = mutableListOf<AgeingBucketZone>()
        val receivableWestBucket = mutableListOf<AgeingBucketZone>()
        val receivableZoneBucket = mutableListOf<AgeingBucketZone>()
        val receivableByAgeViaZone = mutableListOf<ReceivableByAgeViaZone>()
        val receivableFclFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableLclFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableAirFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableFtlFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableLtlFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableHaulageFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableFclCustomsBucket = mutableListOf<AgeingBucketZone>()
        val receivableLclCustomsBucket = mutableListOf<AgeingBucketZone>()
        val receivableAirCustomsBucket = mutableListOf<AgeingBucketZone>()
        val receivableTrailerFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableStoreOrderBucket = mutableListOf<AgeingBucketZone>()
        val receivableAdditionalChargeBucket = mutableListOf<AgeingBucketZone>()
        val receivableFclCfsBucket = mutableListOf<AgeingBucketZone>()
        val receivableOriginServicesBucket = mutableListOf<AgeingBucketZone>()
        val receivableDestinationServicesBucket = mutableListOf<AgeingBucketZone>()
        val receivableFclCustomsFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableLclCustomsFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableAirCustomsFreightBucket = mutableListOf<AgeingBucketZone>()
        val receivableServiceTypeBucket = mutableListOf<AgeingBucketZone>()
        val receivableByAgeViaServiceType = mutableListOf<ReceivableByAgeViaServiceType>()
        var zoneData = listOf<String>()
        var serviceTypeData = listOf<ServiceType>()

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

            val res = AgeingBucketZone(ageingDuration = "", amount = 0.toBigDecimal(), zone = "", serviceType = null, currency = "")

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

        if (request.serviceType?.name.isNullOrEmpty()) {
            ServiceType.values().forEach {
                serviceTypeData = serviceTypeData + it
            }
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "FCL_FREIGHT",
                    ageingBucket = receivableFclFreightBucket,
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "LCL_FREIGHT",
                    ageingBucket = receivableLclFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "AIR_FREIGHT",
                    ageingBucket = receivableAirFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "FTL_FREIGHT",
                    ageingBucket = receivableFtlFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "LTL_FREIGHT",
                    ageingBucket = receivableLtlFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "HAULAGE_FREIGHT",
                    ageingBucket = receivableHaulageFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "FCL_CUSTOMS",
                    ageingBucket = receivableFclCustomsBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "LCL_CUSTOMS",
                    ageingBucket = receivableLclCustomsBucket,
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "AIR_CUSTOMS",
                    ageingBucket = receivableAirCustomsBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "TRAILER_FREIGHT",
                    ageingBucket = receivableTrailerFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "STORE_ORDER",
                    ageingBucket = receivableStoreOrderBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "ADDITIONAL_CHARGE",
                    ageingBucket = receivableAdditionalChargeBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "FCL_CFS",
                    ageingBucket = receivableFclCfsBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "ORIGIN_SERVICES",
                    ageingBucket = receivableOriginServicesBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "DESTINATION_SERVICES",
                    ageingBucket = receivableDestinationServicesBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "FCL_CUSTOMS_FREIGHT",
                    ageingBucket = receivableFclCustomsFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "LCL_CUSTOMS_FREIGHT",
                    ageingBucket = receivableLclCustomsFreightBucket
                )
            )
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = "AIR_CUSTOMS_FREIGHT",
                    ageingBucket = receivableAirCustomsFreightBucket,
                )
            )

            payment.forEach {
                when (it.serviceType) {
                    "FCL_FREIGHT" -> receivableFclFreightBucket.add(receivableBucketAllZone(it))
                    "LCL_FREIGHT" -> receivableLclFreightBucket.add(receivableBucketAllZone(it))
                    "AIR_FREIGHT" -> receivableAirFreightBucket.add(receivableBucketAllZone(it))
                    "FTL_FREIGHT" -> receivableFtlFreightBucket.add(receivableBucketAllZone(it))
                    "LTL_FREIGHT" -> receivableLtlFreightBucket.add(receivableBucketAllZone(it))
                    "HAULAGE_FREIGHT" -> receivableHaulageFreightBucket.add(receivableBucketAllZone(it))
                    "FCL_CUSTOMS" -> receivableFclCustomsBucket.add(receivableBucketAllZone(it))
                    "LCL_CUSTOMS" -> receivableLclCustomsBucket.add(receivableBucketAllZone(it))
                    "AIR_CUSTOMS" -> receivableAirCustomsBucket.add(receivableBucketAllZone(it))
                    "TRAILER_FREIGHT" -> receivableTrailerFreightBucket.add(receivableBucketAllZone(it))
                    "STORE_ORDER" -> receivableStoreOrderBucket.add(receivableBucketAllZone(it))
                    "ADDITIONAL_CHARGE" -> receivableAdditionalChargeBucket.add(receivableBucketAllZone(it))
                    "FCL_CFS" -> receivableFclCfsBucket.add(receivableBucketAllZone(it))
                    "ORIGIN_SERVICES" -> receivableOriginServicesBucket.add(receivableBucketAllZone(it))
                    "DESTINATION_SERVICES" -> receivableDestinationServicesBucket.add(receivableBucketAllZone(it))
                    "FCL_CUSTOMS_FREIGHT" -> receivableFclCustomsFreightBucket.add(receivableBucketAllZone(it))
                    "LCL_CUSTOMS_FREIGHT" -> receivableLclCustomsFreightBucket.add(receivableBucketAllZone(it))
                    "AIR_CUSTOMS_FREIGHT" -> receivableAirCustomsFreightBucket.add(receivableBucketAllZone(it))
                }
            }

            val res = AgeingBucketZone(ageingDuration = "", amount = 0.toBigDecimal(), zone = "", serviceType = null, currency = "")

            if (receivableFclFreightBucket.isEmpty()) {
                receivableFclFreightBucket.add(res)
            }
            if (receivableLclFreightBucket.isEmpty()) {
                receivableLclFreightBucket.add(res)
            }
            if (receivableAirFreightBucket.isEmpty()) {
                receivableAirFreightBucket.add(res)
            }
            if (receivableFtlFreightBucket.isEmpty()) {
                receivableFtlFreightBucket.add(res)
            }
            if (receivableLtlFreightBucket.isEmpty()) {
                receivableLtlFreightBucket.add(res)
            }
            if (receivableHaulageFreightBucket.isEmpty()) {
                receivableHaulageFreightBucket.add(res)
            }
            if (receivableFclCustomsBucket.isEmpty()) {
                receivableFclCustomsBucket.add(res)
            }
            if (receivableLclCustomsBucket.isEmpty()) {
                receivableLclCustomsBucket.add(res)
            }
            if (receivableAirCustomsBucket.isEmpty()) {
                receivableAirCustomsBucket.add(res)
            }
            if (receivableTrailerFreightBucket.isEmpty()) {
                receivableTrailerFreightBucket.add(res)
            }
            if (receivableStoreOrderBucket.isEmpty()) {
                receivableStoreOrderBucket.add(res)
            }
            if (receivableAdditionalChargeBucket.isEmpty()) {
                receivableAdditionalChargeBucket.add(res)
            }
            if (receivableFclCfsBucket.isEmpty()) {
                receivableFclCfsBucket.add(res)
            }
            if (receivableOriginServicesBucket.isEmpty()) {
                receivableOriginServicesBucket.add(res)
            }
            if (receivableDestinationServicesBucket.isEmpty()) {
                receivableDestinationServicesBucket.add(res)
            }
            if (receivableFclCustomsFreightBucket.isEmpty()) {
                receivableFclCustomsFreightBucket.add(res)
            }
            if (receivableLclCustomsFreightBucket.isEmpty()) {
                receivableLclCustomsFreightBucket.add(res)
            }
            if (receivableAirCustomsFreightBucket.isEmpty()) {
                receivableAirCustomsFreightBucket.add(res)
            }

            receivableByAgeViaServiceType[0].ageingBucket = receivableFclFreightBucket
            receivableByAgeViaServiceType[1].ageingBucket = receivableLclFreightBucket
            receivableByAgeViaServiceType[2].ageingBucket = receivableAirFreightBucket
            receivableByAgeViaServiceType[3].ageingBucket = receivableFtlFreightBucket
            receivableByAgeViaServiceType[4].ageingBucket = receivableLtlFreightBucket
            receivableByAgeViaServiceType[5].ageingBucket = receivableHaulageFreightBucket
            receivableByAgeViaServiceType[6].ageingBucket = receivableFclCustomsBucket
            receivableByAgeViaServiceType[7].ageingBucket = receivableLclCustomsBucket
            receivableByAgeViaServiceType[8].ageingBucket = receivableAirCustomsBucket
            receivableByAgeViaServiceType[9].ageingBucket = receivableTrailerFreightBucket
            receivableByAgeViaServiceType[10].ageingBucket = receivableStoreOrderBucket
            receivableByAgeViaServiceType[11].ageingBucket = receivableAdditionalChargeBucket
            receivableByAgeViaServiceType[12].ageingBucket = receivableFclCfsBucket
            receivableByAgeViaServiceType[13].ageingBucket = receivableOriginServicesBucket
            receivableByAgeViaServiceType[14].ageingBucket = receivableDestinationServicesBucket
            receivableByAgeViaServiceType[15].ageingBucket = receivableFclCustomsFreightBucket
            receivableByAgeViaServiceType[16].ageingBucket = receivableLclCustomsFreightBucket
            receivableByAgeViaServiceType[17].ageingBucket = receivableAirCustomsFreightBucket

        } else {
            serviceTypeData = serviceTypeData + listOf(request.serviceType!!)
            receivableByAgeViaServiceType.add(
                ReceivableByAgeViaServiceType(
                    serviceTypeName = request.serviceType?.name,
                    ageingBucket = receivableServiceTypeBucket
                )
            )
            payment.forEach {
                if (it.serviceType == request.serviceType?.name) {
                    receivableServiceTypeBucket.add(receivableBucketAllZone(it))
                }
            }
            receivableByAgeViaServiceType[0].ageingBucket = receivableServiceTypeBucket
        }

        return ReceivableAgeingResponse(
            zone = zoneData,
            receivableByAgeViaZone = receivableByAgeViaZone,
            serviceType = serviceTypeData,
            receivableByAgeViaServiceType = receivableByAgeViaServiceType
        )
    }

    private fun receivableBucketAllZone(response: com.cogoport.ares.api.payment.entity.AgeingBucketZone?): AgeingBucketZone {
        return AgeingBucketZone(
            ageingDuration = response!!.ageingDuration,
            amount = response.amount,
            zone = null,
            serviceType = null,
            currency = response.currency,
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
}
