package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.AresConstants.AIR_SERVICES
import com.cogoport.ares.api.common.AresConstants.ENTITY_ID
import com.cogoport.ares.api.common.AresConstants.OCEAN_SERVICES
import com.cogoport.ares.api.common.AresConstants.SURFACE_SERVICES
import com.cogoport.ares.api.common.AresConstants.TAGGED_ENTITY_ID_MAPPINGS
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.common.models.ServiceLevelOutstanding
import com.cogoport.ares.api.common.models.TradeAndServiceLevelOutstanding
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.entity.DailySalesStats
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.LogisticsMonthlyData
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.model.requests.BfIncomeExpenseReq
import com.cogoport.ares.api.payment.model.requests.BfPendingAmountsReq
import com.cogoport.ares.api.payment.model.requests.BfProfitabilityReq
import com.cogoport.ares.api.payment.model.requests.BfServiceWiseOverdueReq
import com.cogoport.ares.api.payment.model.requests.BfTodayStatReq
import com.cogoport.ares.api.payment.model.requests.ServiceWiseRecPayReq
import com.cogoport.ares.api.payment.model.response.BfIncomeExpenseResponse
import com.cogoport.ares.api.payment.model.response.BfTodayStatsResp
import com.cogoport.ares.api.payment.model.response.OnAccountAndOutstandingResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseOverdueResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseRecPayResp
import com.cogoport.ares.api.payment.model.response.ShipmentProfitResp
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.utils.toLocalDate
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.OverallStatsForCustomers
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DocumentType
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.PayableAgeingBucket
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.DailyStatsRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.InvoiceTatStatsRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.SalesFunnelRequest
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.PayableOutstandingResponse
import com.cogoport.ares.model.payment.response.QsoResponse
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
import java.time.LocalDate.now
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

@Singleton
class DashboardServiceImpl : DashboardService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var overallAgeingConverter: OverallAgeingMapper

    @Inject
    lateinit var businessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    private suspend fun getDefaultersOrgIds(): List<UUID>? {
        return businessPartnersServiceImpl.listTradePartyDetailIds()
    }

    override suspend fun deleteIndex(index: String) {
        Client.deleteIndex(index)
    }

    override suspend fun createIndex(index: String) {
        Client.createIndex(index)
    }

    override suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): LinkedHashMap<String, OverallAgeingStatsResponse> {
        val defaultersOrgIds = getDefaultersOrgIds()
        val entityCode = request.entityCode ?: 301

        val updatedCompanyType = getCompanyType(request.companyType)

        val ledgerCurrency = AresConstants.LEDGER_CURRENCY[entityCode]
        val outstandingResponse = unifiedDBRepo.getOutstandingByAge(request.serviceType, defaultersOrgIds, updatedCompanyType, entityCode)

        val durationKey = listOf("Not Due", "1-30", "31-60", "61-90", "91-180", "181-365", ">365")

        val formattedData = LinkedHashMap<String, OverallAgeingStatsResponse>()

        if (outstandingResponse.isEmpty()) {
            durationKey.map {
                formattedData[it] = OverallAgeingStatsResponse(
                    ageingDuration = it,
                    amount = 0.toBigDecimal(),
                    dashboardCurrency = ledgerCurrency!!
                )
            }
            return formattedData
        }

        val data = mutableListOf<OverallAgeingStatsResponse>()

        outstandingResponse.map { response ->
            response.amount = response.amount.setScale(4, RoundingMode.UP)
            data.add(overallAgeingConverter.convertToModel(response))
        }

        durationKey.map { key ->
            val durationData = data.filter { it.ageingDuration == key }
            if (!durationData.isNullOrEmpty()) {
                formattedData.put(key, durationData[0])
            } else {
                formattedData[key] = OverallAgeingStatsResponse(
                    ageingDuration = key,
                    amount = 0.toBigDecimal(),
                    dashboardCurrency = ledgerCurrency!!
                )
            }
        }

        formattedData.values.sortedBy { it.ageingDuration }

        return formattedData
    }

    override suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding {
        val serviceType = request.serviceType
        val entityCode = request.entityCode ?: 301
        val year = request.year ?: AresConstants.CURR_YEAR
        val dashboardCurrency = AresConstants.LEDGER_CURRENCY[entityCode]

        val updatedCompanyType = getCompanyType(request.companyType)

        val companyType = request.companyType

        val quarterMapping = mapOf(
            1 to "JAN - MAR",
            2 to "APR - JUN",
            3 to "JUL - SEPT",
            4 to "OCT - DEC"
        )

        val defaultersOrgIds = getDefaultersOrgIds()

        val quarterOutstandingData = unifiedDBRepo.generateQuarterlyOutstanding(year, serviceType, defaultersOrgIds, entityCode, updatedCompanyType)
        val qsoResponseList = mutableListOf<QsoResponse>()

        (1..4).toList().map { quarter ->
            val data = quarterOutstandingData?.filter { it.duration == quarter.toString() }
            if (!data.isNullOrEmpty()) {
                val monthList = getMonthFromQuarter(quarter)
                val updatedData = data[0]
                var numberOfDays = 0
                monthList.map { month ->
                    val days = when (month.toInt() == AresConstants.CURR_MONTH) {
                        true -> AresConstants.CURR_DATE.toLocalDate()?.dayOfMonth!!
                        else -> YearMonth.of(year, month.toInt()).lengthOfMonth()
                    }
                    numberOfDays = numberOfDays.plus(days)
                }
                val qso = when (updatedData.totalSales != BigDecimal.ZERO) {
                    true -> {
                        updatedData.totalOutstandingAmount?.div(updatedData.totalSales!!)?.times(numberOfDays.toBigDecimal())!!
                    }
                    else -> {
                        0.toBigDecimal()
                    }
                }
                val qsoResponseData = QsoResponse(
                    quarter = quarterMapping[quarter]!!,
                    qsoForQuarter = qso,
                    currency = dashboardCurrency
                )
                qsoResponseList.add(qsoResponseData)
            } else {
                val qsoResponseData = QsoResponse(
                    quarter = quarterMapping[quarter]!!,
                    qsoForQuarter = 0.toBigDecimal(),
                    currency = dashboardCurrency
                )
                qsoResponseList.add(qsoResponseData)
            }
        }

        return QuarterlyOutstanding(
            list = qsoResponseList
        )
    }

    override suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding {
        val defaultersOrgIds = getDefaultersOrgIds()
        val entityCode = request.entityCode ?: 301
        val dashboardCurrency = AresConstants.LEDGER_CURRENCY[entityCode]
        val year = request.year ?: AresConstants.CURR_YEAR
        val serviceType = request.serviceType
        val companyType = request.companyType
        val monthList = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        val updatedCompanyType = getCompanyType(companyType)

        val dailySalesZoneServiceTypeData = unifiedDBRepo.generateDailySalesOutstanding(year, serviceType, defaultersOrgIds, entityCode, updatedCompanyType)

        val dsoResponse = mutableListOf<DsoResponse>()

        (1..12).toList().map { month ->
            val data = dailySalesZoneServiceTypeData.filter { it.month == month }
            if (data.isNotEmpty()) {
                val updatedData = data[0]
                val dso = when (updatedData.totalSales != BigDecimal.ZERO) {
                    true -> {
                        val days = when (updatedData.month == AresConstants.CURR_MONTH) {
                            true -> AresConstants.CURR_DATE.toLocalDate()?.dayOfMonth
                            else -> YearMonth.of(year, updatedData.month).lengthOfMonth()
                        }
                        updatedData.outstandings?.div(updatedData.totalSales!!)?.times(days?.toBigDecimal()!!)!!
                    }
                    else -> {
                        0.toBigDecimal()
                    }
                }
                val dailyOutstandingResponseData = DsoResponse(
                    month = monthList[month - 1],
                    dsoForTheMonth = dso,
                    currency = dashboardCurrency
                )
                dsoResponse.add(dailyOutstandingResponseData)
            } else {
                val dailyOutstandingResponseData = DsoResponse(
                    month = monthList[month - 1],
                    dsoForTheMonth = 0.toBigDecimal(),
                    currency = dashboardCurrency
                )
                dsoResponse.add(dailyOutstandingResponseData)
            }
        }

        return DailySalesOutstanding(
            dsoResponse = dsoResponse
        )
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
                amount = it.aggregations()["currAmount"]?.sum()?.value()?.toBigDecimal()!!,
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

    override suspend fun getSalesFunnel(req: SalesFunnelRequest): SalesFunnelResponse {
        val entityCode = req.entityCode ?: 301
        val serviceType = req.serviceType
        val month = req.month
        val companyType = req.companyType
        val year = req.year ?: AresModelConstants.CURR_YEAR

        val updatedCompanyType = getCompanyType(companyType)

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        val monthKey = when (!month.isNullOrEmpty()) {
            true -> months.indexOf(month) + 1
            else -> AresModelConstants.CURR_MONTH
        }

        val salesFunnelResponse = SalesFunnelResponse()

        val data = unifiedDBRepo.getFunnelData(entityCode, updatedCompanyType, serviceType?.name?.lowercase(), year, monthKey)

        if (data?.size != 0) {
            salesFunnelResponse.draftInvoicesCount = data?.size
            salesFunnelResponse.financeAcceptedInvoiceCount = data?.count { it.status?.name != "DRAFT" }
            salesFunnelResponse.irnGeneratedInvoicesCount =
                data?.count { !listOf("DRAFT", "FINANCE_ACCEPTED").contains(it.status?.name) }
            salesFunnelResponse.settledInvoicesCount = data?.count { it.paymentStatus == "PAID" }

            salesFunnelResponse.draftToFinanceAcceptedPercentage =
                salesFunnelResponse.financeAcceptedInvoiceCount?.times(100)
                    ?.div(salesFunnelResponse.draftInvoicesCount!!)

            if (salesFunnelResponse.financeAcceptedInvoiceCount!! != 0) {
                salesFunnelResponse.financeToIrnPercentage = salesFunnelResponse.irnGeneratedInvoicesCount?.times(100)
                    ?.div(salesFunnelResponse.financeAcceptedInvoiceCount!!)
            }
            if (salesFunnelResponse.irnGeneratedInvoicesCount!! != 0) {
                salesFunnelResponse.settledPercentage = salesFunnelResponse.settledInvoicesCount?.times(100)
                    ?.div(salesFunnelResponse.irnGeneratedInvoicesCount!!)
            }
        }

        return salesFunnelResponse
    }

    override suspend fun getInvoiceTatStats(req: InvoiceTatStatsRequest): InvoiceTatStatsResponse {
        val serviceType = req.serviceType
        val companyType = req.companyType
        var countIrnGeneratedEvent: Int? = 0
        val entityCode = req.entityCode ?: 301

        val month = req.month
        val year = req.year ?: AresModelConstants.CURR_YEAR

        val updatedCompanyType = getCompanyType(companyType)

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        val monthKey = when (!month.isNullOrEmpty()) {
            true -> months.indexOf(month) + 1
            else -> AresModelConstants.CURR_MONTH
        }

        val data = unifiedDBRepo.getInvoiceTatStats(year, monthKey, entityCode, updatedCompanyType, serviceType?.name?.lowercase())

        val objectMapper = ObjectMapper()

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val mapOfData = mapOf("finance_accepted" to data.filter { it.status != "DRAFT" }, "irn_generated" to data.filter { !listOf("DRAFT", "FINANCE_ACCEPTED").contains(it.status) }, "settled" to data.filter { it.paymentStatus == "PAID" })

        val invoiceTatStatsResponse = InvoiceTatStatsResponse()

        mapOfData.entries.map { (status, invoices) ->
            invoices.map { invoice ->
                val eventData = objectMapper.readValue(invoice.events, Array<InvoiceEventResponse>::class.java)
                when (status) {
                    "finance_accepted" -> {
                        val createdAtEventDate = eventData.first { it.eventName == "CREATED" }.occurredAt.time
                        var financeAcceptedEventDate = 0L
                        if (eventData.map { it.eventName }.contains("FINANCE_ACCEPTED")) {
                            financeAcceptedEventDate = eventData.first { it.eventName == "FINANCE_ACCEPTED" }.occurredAt.time
                        }
                        if (financeAcceptedEventDate != 0L) {
                            invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted = invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted?.plus(financeAcceptedEventDate.minus(createdAtEventDate))
                            invoiceTatStatsResponse.financeAcceptedInvoiceEventCount = invoiceTatStatsResponse.financeAcceptedInvoiceEventCount?.plus(1)
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
                            invoiceTatStatsResponse.irnGeneratedInvoiceEventCount = invoiceTatStatsResponse.irnGeneratedInvoiceEventCount?.plus(1)
                        }

                        if (irnGeneratedEventDate != 0L && financeAcceptedEventDate != 0L && irnGeneratedEventDate >= financeAcceptedEventDate) {
                            invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated?.plus(irnGeneratedEventDate.minus(financeAcceptedEventDate))
                            countIrnGeneratedEvent = countIrnGeneratedEvent?.plus(1)
                        } else {}
                    }
                    "settled" -> {
                        var irnGeneratedEventDate = 0L
                        var settlementEventDate = 0L
                        if (eventData.map { it.eventName }.contains("IRN_GENERATED")) {
                            irnGeneratedEventDate = eventData.first { it.eventName == "IRN_GENERATED" }.occurredAt.time
                        }
                        if (eventData.map { it.eventName }.contains("PAID")) {
                            settlementEventDate =
                                eventData.first { it.eventName == "PAID" }.occurredAt.time
                        }

                        if (settlementEventDate != 0L) {
                            invoiceTatStatsResponse.tatHoursFromIrnGeneratedToSettled?.plus(
                                settlementEventDate.minus(
                                    irnGeneratedEventDate
                                )
                            )
                            invoiceTatStatsResponse.settledInvoiceEventCount = invoiceTatStatsResponse.settledInvoiceEventCount?.plus(1)
                        }
                    }
                    else -> {}
                }
            }
        }

        invoiceTatStatsResponse.draftInvoicesCount = data.size
        invoiceTatStatsResponse.financeAcceptedInvoiceCount = mapOfData["finance_accepted"]?.size
        invoiceTatStatsResponse.irnGeneratedInvoicesCount = mapOfData["irn_generated"]?.size
        invoiceTatStatsResponse.settledInvoicesCount = mapOfData["settled"]?.size

        invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted = invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted?.div(1000)?.div(60)?.div(60)
        invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated?.div(1000)?.div(60)?.div(60)
        invoiceTatStatsResponse.tatHoursFromIrnGeneratedToSettled = invoiceTatStatsResponse.tatHoursFromIrnGeneratedToSettled?.div(1000)?.div(60)?.div(60)

        if (invoiceTatStatsResponse.financeAcceptedInvoiceEventCount != 0) {
            invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted = invoiceTatStatsResponse.tatHoursFromDraftToFinanceAccepted?.div(invoiceTatStatsResponse.financeAcceptedInvoiceEventCount!!)
        }

        if (invoiceTatStatsResponse.irnGeneratedInvoiceEventCount != 0) {
            invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated = invoiceTatStatsResponse.tatHoursFromFinanceAcceptedToIrnGenerated?.div(countIrnGeneratedEvent!!)
        }
        if (invoiceTatStatsResponse.settledInvoiceEventCount != 0) {
            invoiceTatStatsResponse.tatHoursFromIrnGeneratedToSettled?.div(invoiceTatStatsResponse.settledInvoiceEventCount!!)
        }

        return invoiceTatStatsResponse
    }

    override suspend fun getOutstanding(entityCode: Int?): OutstandingOpensearchResponse {
        val openSearchData = OutstandingOpensearchResponse(
            null,
            null
        )

        val updatedEntityCode = entityCode ?: 301

        val dashboardCurrency = AresConstants.LEDGER_CURRENCY[updatedEntityCode]

        val defaultersOrgIds = getDefaultersOrgIds()

        val possibleServiceAndTradeType = mapOf(
            "ocean" to listOf("FCL_FREIGHT_IMPORT", "FCL_FREIGHT_EXPORT", "LCL_FREIGHT_IMPORT", "LCL_FREIGHT_EXPORT"),
            "air" to listOf("AIR_CUSTOMS_IMPORT", "AIR_FREIGHT_IMPORT", "AIR_CUSTOMS_EXPORT", "AIR_FREIGHT_EXPORT"),
            "surface" to listOf("FTL_FREIGHT_IMPORT", "FTL_FREIGHT_EXPORT", "LTL_FREIGHT_IMPORT", "LTL_FREIGHT_EXPORT")
        )

        val data = unifiedDBRepo.getOutstandingData(updatedEntityCode, defaultersOrgIds)
        val mapData = hashMapOf<String, ServiceLevelOutstanding> ()

        if (data.isNullOrEmpty()) {
            possibleServiceAndTradeType.entries.map { (k, v) ->
                mapData[k] = ServiceLevelOutstanding(
                    openInvoiceAmount = BigDecimal.ZERO,
                    currency = dashboardCurrency,
                    tradeType = v.map { value ->
                        TradeAndServiceLevelOutstanding(
                            key = value,
                            name = value.replace("_", " "),
                            openInvoiceAmount = BigDecimal.ZERO,
                            currency = dashboardCurrency
                        )
                    }
                )
            }
            openSearchData.overallStats = OverallStats(
                dashboardCurrency = dashboardCurrency!!
            )

            openSearchData.outstandingServiceWise = mapData

            return openSearchData
        }

        val onAccountAmount = unifiedDBRepo.getOnAccountAmount(mutableListOf(updatedEntityCode), defaultersOrgIds, "AR", listOf("REC", "CTDS", "BANK", "CONTR", "ROFF", "MTCCV", "MISC", "INTER", "OPDIV", "MTC", "PAY"))
        val onAccountAmountForPastSevenDays = unifiedDBRepo.getOnAccountAmountForPastSevenDays(updatedEntityCode, defaultersOrgIds)
        val openInvoiceAmountForPastSevenDays = unifiedDBRepo.getOutstandingAmountForPastSevenDays(updatedEntityCode, defaultersOrgIds)

        data.map { it.tradeType = it.tradeType?.uppercase() }
        data.map { it.serviceType = it.serviceType?.uppercase() }

        data.groupBy { it.groupedServices }.filter { it.key != null }.entries.map { (k, v) ->
            mapData[k.toString()] = ServiceLevelOutstanding(
                openInvoiceAmount = v.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP),
                currency = dashboardCurrency,
                tradeType = getTradeAndServiceWiseData(v)
            )
        }

        val onAccountAmountForPastSevenDaysPercentage = when (onAccountAmount != BigDecimal.ZERO) {
            true -> onAccountAmountForPastSevenDays?.div(onAccountAmount?.setScale(4, RoundingMode.UP)!!)
                ?.times(100.toBigDecimal())?.toLong()
            else -> BigDecimal.ZERO
        }

        val totalOutstandingAmount = data.sumOf { it.openInvoiceAmount }.minus(onAccountAmount?.multiply(BigDecimal(-1))!!)

        openSearchData.outstandingServiceWise = mapData
        openSearchData.overallStats = OverallStats(
            totalOutstandingAmount = totalOutstandingAmount.setScale(4, RoundingMode.UP),
            openInvoicesAmount = data.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP),
            customersCount = data.sumOf { it.customersCount!! },
            dashboardCurrency = data.first().currency!!,
            openInvoicesCount = data.sumOf { it.openInvoicesCount!! },
            openInvoiceAmountForPastSevenDaysPercentage = openInvoiceAmountForPastSevenDays?.div(data.sumOf { it.openInvoiceAmount }.setScale(4, RoundingMode.UP))?.times(100.toBigDecimal())?.toLong(),
            onAccountAmount = onAccountAmount.setScale(4, RoundingMode.UP),
            onAccountAmountForPastSevenDaysPercentage = onAccountAmountForPastSevenDaysPercentage?.toLong()
        )

        return openSearchData
    }
    private fun getTradeAndServiceWiseData(value: List<OutstandingDocument>): List<TradeAndServiceLevelOutstanding> {
        val updatedList = mutableListOf<TradeAndServiceLevelOutstanding>()
        value.map { item ->
            if (item.serviceType == null || item.tradeType == null) {
                val document = updatedList.filter { it.key == "others" }
                if (document.isNotEmpty()) {
                    document.first().openInvoiceAmount = document.first().openInvoiceAmount.plus(item.openInvoiceAmount).setScale(4, RoundingMode.UP)
                } else {
                    val tradeAndServiceWiseDocument = TradeAndServiceLevelOutstanding(
                        key = "others",
                        name = "others",
                        openInvoiceAmount = item.openInvoiceAmount.setScale(4, RoundingMode.UP),
                        currency = item.currency
                    )
                    updatedList.add(tradeAndServiceWiseDocument)
                }
            } else {
                val document = updatedList.filter { it.key == "${item.serviceType}_${item.tradeType}" }
                if (document.isNotEmpty()) {
                    document.first().openInvoiceAmount = document.first().openInvoiceAmount.plus(item.openInvoiceAmount).setScale(4, RoundingMode.UP)
                } else {
                    val tradeAndServiceWiseDocument = TradeAndServiceLevelOutstanding(
                        key = "${item.serviceType}_${item.tradeType}",
                        name = "${item.serviceType} ${item.tradeType}",
                        openInvoiceAmount = item.openInvoiceAmount.setScale(4, RoundingMode.UP),
                        currency = item.currency
                    )
                    updatedList.add(tradeAndServiceWiseDocument)
                }
            }
        }
        return updatedList
    }

    override suspend fun getDailySalesStatistics(req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>> {
        val month = req.month
        var year = req.year
        val asOnDate = req.asOnDate
        val serviceType = req.serviceType
        val companyType = req.companyType
        val documentType = req.documentType ?: DocumentType.SALES_INVOICE
        val entityCode = req.entityCode ?: 301
        val dashboardCurrency = AresConstants.LEDGER_CURRENCY[entityCode]

        val updatedCompanyType = getCompanyType(companyType)

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")

        var dailySalesStats = mutableListOf<DailySalesStats>()

        val hashMap = hashMapOf<String, ArrayList<DailySalesStats>>()

        if (year != null && month == null) {
            val endDate = "$year-12-31".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            dailySalesStats = if (documentType != DocumentType.SHIPMENT_CREATED) {
                unifiedDBRepo.generateYearlySalesStats(
                    endDate,
                    getAccTypeAnDocStatus(documentType),
                    entityCode,
                    updatedCompanyType,
                    serviceType?.name?.lowercase()
                )!!
            } else {
                unifiedDBRepo.generateYearlyShipmentCreatedAt(endDate, entityCode, updatedCompanyType, serviceType?.name?.lowercase())!!
            }
        } else {
            if ((month != null && year != null) || (month != null && year == null)) {
                year = year ?: AresConstants.CURR_YEAR
                val monthInt = generateMonthKeyIndex(months.indexOf(month) + 1)
                val quarterStart = YearMonth.of(year, monthInt.toInt()).minusMonths(3).atDay(1).atStartOfDay()
                val quarterEnd = YearMonth.of(year, monthInt.toInt()).plusMonths(1).atDay(1).atStartOfDay()

                dailySalesStats = if (documentType != DocumentType.SHIPMENT_CREATED) {
                    unifiedDBRepo.generateMonthlySalesStats(
                        quarterStart,
                        quarterEnd,
                        getAccTypeAnDocStatus(documentType),
                        entityCode,
                        updatedCompanyType,
                        serviceType?.name?.lowercase()
                    )!!
                } else {
                    unifiedDBRepo.generateMonthlyShipmentCreatedAt(quarterStart, quarterEnd, entityCode, updatedCompanyType, serviceType?.name?.lowercase())!!
                }
            } else {
                val endDate = asOnDate ?: "${AresConstants.CURR_YEAR}-${generateMonthKeyIndex(AresConstants.CURR_MONTH)}-${generateMonthKeyIndex(AresConstants.CURR_DATE.toLocalDateTime().dayOfMonth)}".format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                dailySalesStats = if (documentType != DocumentType.SHIPMENT_CREATED) {
                    unifiedDBRepo.generateDailySalesStats(
                        endDate,
                        getAccTypeAnDocStatus(documentType),
                        entityCode,
                        updatedCompanyType,
                        serviceType?.name?.lowercase()
                    )!!
                } else {
                    unifiedDBRepo.generateDailyShipmentCreatedAt(endDate, entityCode, updatedCompanyType, serviceType?.name?.lowercase())!!
                }
            }
        }

        if (!dailySalesStats.isNullOrEmpty()) {
            dailySalesStats.groupBy { it -> it.duration }.entries.map { (key, value) ->

                value.map { item ->
                    val dailySalesStats = DailySalesStats(
                        amount = 0.toBigDecimal(),
                        duration = key,
                        dashboardCurrency = dashboardCurrency,
                        count = 0L
                    )

                    if (documentType == DocumentType.SALES_INVOICE) {
                        dailySalesStats.amount = dailySalesStats.amount.plus(item.amount)
                        dailySalesStats.count = dailySalesStats.count?.plus(item.count!!)
                        dailySalesStats.invoiceType = item.invoiceType

                        if (hashMap.keys.contains(documentType.name)) {
                            hashMap[documentType.name]?.add(dailySalesStats)
                        } else {
                            hashMap[documentType.name] = arrayListOf(dailySalesStats)
                        }
                    } else {
                        dailySalesStats.amount = dailySalesStats.amount.plus(item.amount)
                        dailySalesStats.count = dailySalesStats.count?.plus(item.count!!)
                        dailySalesStats.invoiceType = item.invoiceType

                        if (hashMap.keys.contains(documentType.name)) {
                            hashMap[documentType.name]?.add(dailySalesStats)
                        } else {
                            hashMap[documentType.name] = arrayListOf(dailySalesStats)
                        }
                    }
                }
            }
        }
        if (documentType == DocumentType.SALES_INVOICE) {
            hashMap[documentType.name]?.groupBy { it.duration }?.entries?.map { (k, v) ->
                listOf("INVOICE", "CREDIT_NOTE").map { type ->
                    if (v.none { value -> value.invoiceType == type }) {
                        val dummyEntry = DailySalesStats(
                            amount = BigDecimal.ZERO,
                            duration = k,
                            dashboardCurrency = dashboardCurrency,
                            count = 0,
                            invoiceType = type
                        )
                        hashMap[documentType.name]?.add(dummyEntry)
                    }
                }
            }

            hashMap[documentType.name]?.removeIf { it.invoiceType == null }

            val data = hashMap[documentType.name]?.groupBy { it.duration }?.values?.toMutableList()

            data?.map {
                val revenue = DailySalesStats(
                    amount = it.first { it.invoiceType == "INVOICE" }.amount.minus(it.first { it.invoiceType == "CREDIT_NOTE" }.amount),
                    duration = it[0].duration,
                    dashboardCurrency = it[0].dashboardCurrency,
                    count = 0,
                    invoiceType = "REVENUE"
                )

                hashMap[documentType.name]?.add(revenue)
            }
        }

        hashMap[documentType.name]?.sortedBy { it.duration }

        return hashMap
    }

    override suspend fun getKamWiseOutstanding(entityCode: Int?, companyType: CompanyType?, serviceType: ServiceType?): List<KamWiseOutstanding>? {
        if (entityCode in listOf(201, 401)) {
            return listOf()
        }
        val defaultersOrgIds = getDefaultersOrgIds()
        val updatedCompanyType = getCompanyType(companyType)

        val stakeholderIds = AresConstants.KAM_OWNERS_LIST_ENTITY_CODE_MAPPING[entityCode]?.map { UUID.fromString(it) }

        val kamWiseData = unifiedDBRepo.getKamWiseOutstanding(entityCode, serviceType, updatedCompanyType, defaultersOrgIds, stakeholderIds)

        if (!kamWiseData.isNullOrEmpty()) {
            kamWiseData.map {
                it.dashboardCurrency = AresConstants.LEDGER_CURRENCY[entityCode]
                it.entityCode = entityCode
            }
        }

        return kamWiseData
    }

    private fun generateMonthKeyIndex(month: Int): String {
        return when (month < 10) {
            true -> "0$month"
            else -> month.toString()
        }
    }

    override suspend fun getLineGraphViewDailyStats(req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>> {
        val serviceType = req.serviceType
        val companyType = req.companyType
        val asOnDate = (req.asOnDate ?: AresConstants.CURR_DATE.toString()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val documentType = req.documentType ?: DocumentType.SALES_INVOICE
        val entityCode = req.entityCode ?: 301
        val dashboardCurrency = AresConstants.LEDGER_CURRENCY[entityCode]

        val defaultersOrgIds = getDefaultersOrgIds()

        val hashMap = hashMapOf<String, ArrayList<DailySalesStats>>()

        val updatedCompanyType = getCompanyType(companyType)

        val dailySalesStats = if (req.documentType != DocumentType.SHIPMENT_CREATED) {
            unifiedDBRepo.generateLineGraphViewDailyStats(
                asOnDate,
                getAccTypeAnDocStatus(documentType),
                defaultersOrgIds,
                entityCode,
                updatedCompanyType,
                serviceType?.name?.lowercase()
            )!!
        } else {
            unifiedDBRepo.generateLineGraphViewShipmentCreated(asOnDate, entityCode, updatedCompanyType, req.serviceType?.name?.lowercase())!!
        }

        if (!dailySalesStats.isNullOrEmpty()) {
            dailySalesStats.groupBy { it -> it.duration }.entries.map { (key, value) ->

                value.map { item ->
                    val dailySalesStats = DailySalesStats(
                        amount = 0.toBigDecimal(),
                        duration = key,
                        dashboardCurrency = dashboardCurrency,
                        count = 0L
                    )

                    if (documentType == DocumentType.SALES_INVOICE) {
                        dailySalesStats.amount = dailySalesStats.amount.plus(item.amount)
                        dailySalesStats.count = dailySalesStats.count?.plus(item.count!!)
                        dailySalesStats.invoiceType = item.invoiceType

                        if (hashMap.keys.contains(documentType.name)) {
                            hashMap[documentType.name]?.add(dailySalesStats)
                        } else {
                            hashMap[documentType.name] = arrayListOf(dailySalesStats)
                        }
                    } else {
                        dailySalesStats.amount = dailySalesStats.amount.plus(item.amount)
                        dailySalesStats.count = dailySalesStats.count?.plus(item.count!!)
                        dailySalesStats.invoiceType = item.invoiceType

                        if (hashMap.keys.contains(documentType.name)) {
                            hashMap[documentType.name]?.add(dailySalesStats)
                        } else {
                            hashMap[documentType.name] = arrayListOf(dailySalesStats)
                        }
                    }
                }
            }
        }
        if (documentType == DocumentType.SALES_INVOICE) {
            hashMap[documentType.name]?.groupBy { it.duration }?.entries?.map { (k, v) ->
                listOf("INVOICE", "CREDIT_NOTE").map { type ->
                    if (v.none { value -> value.invoiceType == type }) {
                        val dummyEntry = DailySalesStats(
                            amount = BigDecimal.ZERO,
                            duration = k,
                            dashboardCurrency = dashboardCurrency,
                            count = 0,
                            invoiceType = type
                        )
                        hashMap[documentType.name]?.add(dummyEntry)
                    }
                }
            }

            hashMap[documentType.name]?.removeIf { it.invoiceType == null }

            val data = hashMap[documentType.name]?.groupBy { it.duration }?.values?.toMutableList()

            data?.map {
                val revenue = DailySalesStats(
                    amount = it.first { it.invoiceType == "INVOICE" }.amount.minus(it.first { it.invoiceType == "CREDIT_NOTE" }.amount),
                    duration = it[0].duration,
                    dashboardCurrency = it[0].dashboardCurrency,
                    count = 0,
                    invoiceType = "REVENUE"
                )

                hashMap[documentType.name]?.add(revenue)
            }
        }

        return hashMap
    }

    private fun getAccTypeAnDocStatus(documentType: DocumentType): List<String>? {
        val accTypeDocStatusMapping = mapOf(
            DocumentType.SALES_INVOICE to listOf("INVOICE", "CREDIT_NOTE"),
            DocumentType.CREDIT_NOTE to listOf("CREDIT_NOTE")
        )
        return accTypeDocStatusMapping[documentType]
    }

    override suspend fun getFinanceReceivableData(request: BfPendingAmountsReq): BfReceivableAndPayable {
        val response: BfReceivableAndPayable?
        val onAccountPayment: BigDecimal?
        val pieChartData: MutableList<OnAccountAndOutstandingResp> = mutableListOf()
        var receivableOrPayableTillYesterday: BigDecimal?
        val onAccountTillYesterday: BigDecimal?
        if (request.accountMode == AccMode.AP) {
            response = unifiedDBRepo.getBfPayable(
                request.serviceTypes, request.startDate,
                request.endDate, request.tradeType, request.entityCode,
            )
            receivableOrPayableTillYesterday = response.tillYesterdayTotalOutstanding
            onAccountPayment = unifiedDBRepo.getOnAccountAmount(request.entityCode, null, "AP", listOf("PAY"), request.serviceTypes, request.startDate, request.endDate)
            onAccountTillYesterday = unifiedDBRepo.getOnAccountAmount(request.entityCode, null, "AP", listOf("PAY"), request.serviceTypes, request.startDate, request.endDate, true)
        } else {
            val defaultOrgIds = getDefaultersOrgIds()
            val customerTypes = mapOf(
                "cp" to listOf("channel_partner"),
                "ie" to listOf("mid_size", "long_tail"),
                "enterprise" to listOf("enterprise")
            )
            response = unifiedDBRepo.getBfReceivable(
                request.serviceTypes, request.startDate, request.endDate,
                request.tradeType, request.entityCode, customerTypes[request.buyerType],
                defaultOrgIds
            )
            receivableOrPayableTillYesterday = response.tillYesterdayTotalOutstanding
            onAccountPayment = unifiedDBRepo.getOnAccountAmount(request.entityCode, defaultOrgIds, "AR", listOf("REC", "CTDS", "BANK", "CONTR", "ROFF", "MTCCV", "MISC", "INTER", "OPDIV", "MTC"), request.serviceTypes, request.startDate, request.endDate)
            onAccountTillYesterday = unifiedDBRepo.getOnAccountAmount(request.entityCode, defaultOrgIds, "AR", listOf("REC", "CTDS", "BANK", "CONTR", "ROFF", "MTCCV", "MISC", "INTER", "OPDIV", "MTC"), request.serviceTypes, request.startDate, request.endDate, true)
        }
        var totalReceivableOrPayable = response.overdueAmount?.plus(response.nonOverdueAmount!!)
        if (request.accountMode == AccMode.AP) {
            totalReceivableOrPayable = totalReceivableOrPayable?.times((-1).toBigDecimal())
            receivableOrPayableTillYesterday = receivableOrPayableTillYesterday?.times((-1).toBigDecimal())
        }
        val totalOutStanding = totalReceivableOrPayable?.plus(onAccountPayment!!)
        val totalOutStandingTillYesterday = receivableOrPayableTillYesterday?.plus(onAccountTillYesterday ?: 0.toBigDecimal())
        val onAccountPaymentChangeFromYesterday = onAccountPayment?.minus(onAccountTillYesterday!!)
        val totalOutStandingChangeFromYesterday = totalOutStanding?.minus(totalOutStandingTillYesterday!!)
        response.onAccountChangeFromYesterday = onAccountPaymentChangeFromYesterday?.let {
            onAccountTillYesterday.takeIf { it != BigDecimal.ZERO }?.let {
                (onAccountPaymentChangeFromYesterday.divide(onAccountTillYesterday?.abs(), 5, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100))
            }
        } ?: BigDecimal.ZERO
        response.outstandingChangeFromYesterday = totalOutStandingChangeFromYesterday?.let {
            totalOutStandingTillYesterday.takeIf { it != BigDecimal.ZERO }?.let {
                (totalOutStandingChangeFromYesterday.divide(totalOutStandingTillYesterday?.abs(), 5, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100))
            }
        } ?: BigDecimal.ZERO
        pieChartData.add(OnAccountAndOutstandingResp("outstanding", totalOutStanding))
        pieChartData.add(OnAccountAndOutstandingResp("onAccount", onAccountPayment))
        response.onAccountAndOutStandingData = pieChartData
        return response
    }

    override suspend fun getFinanceIncomeExpense(request: BfIncomeExpenseReq): MutableList<BfIncomeExpenseResponse> {
        val thisYear = Year.now().toString()
        if ((request.financeYearStart == null && request.financeYearEnd != null) || (request.financeYearStart != null && request.financeYearEnd == null)) {
            throw AresException(AresError.ERR_1006, "One of the finance Year is null")
        }
        val startYear = request.financeYearStart ?: request.calenderYear ?: thisYear
        var endYear = request.financeYearEnd ?: request.calenderYear ?: thisYear
        var isLeapYear = Year.isLeap(endYear.toLong())

        val monthlyIncomes = unifiedDBRepo.getBfIncomeMonthly(
            request.serviceTypes,
            startYear,
            endYear,
            request.isPostTax!!,
            request.entityCode,
            isLeapYear
        )
        val monthlyExpenses = unifiedDBRepo.getBfExpenseMonthly(
            request.serviceTypes,
            startYear,
            endYear,
            request.isPostTax!!,
            request.entityCode,
            isLeapYear
        )
        var response = mutableListOf<BfIncomeExpenseResponse>()
        for (monthIndex in 1..12) {
            var monthName = Month.of(monthIndex)
            response.add(
                BfIncomeExpenseResponse(
                    month = monthName,
                    income = getMonthData(monthlyIncomes, monthName),
                    expense = getMonthData(monthlyExpenses, monthName)
                )
            )
        }
        if (request.financeYearStart != null) {
            for (index in 0..2) {
                val monthData = response.removeAt(0)
                response.add(monthData)
            }
        }
        return response
    }

    private fun getMonthData(data: LogisticsMonthlyData, month: Month): BigDecimal? {
        return when (month) {
            Month.JANUARY -> data.january
            Month.FEBRUARY -> data.february
            Month.MARCH -> data.march
            Month.APRIL -> data.april
            Month.MAY -> data.may
            Month.JUNE -> data.june
            Month.JULY -> data.july
            Month.AUGUST -> data.august
            Month.SEPTEMBER -> data.september
            Month.OCTOBER -> data.october
            Month.NOVEMBER -> data.november
            Month.DECEMBER -> data.december
            else -> null
        }
    }

    override suspend fun getFinanceTodayStats(request: BfTodayStatReq): BfTodayStatsResp {
        val todaySalesData = unifiedDBRepo.getSalesStatsByDate(request.serviceTypes, request.entityCode, now())
        val todayPurchaseData = unifiedDBRepo.getPurchaseStatsByDate(request.serviceTypes, request.entityCode, now())
        val response = BfTodayStatsResp(
            todaySalesStats = todaySalesData,
            todayPurchaseStats = todayPurchaseData,
        )
        var yesterday = now().minus(1, ChronoUnit.DAYS)
        val yesterdaySalesData = unifiedDBRepo.getSalesStatsByDate(request.serviceTypes, request.entityCode, yesterday)
        val yesterdayPurchaseData = unifiedDBRepo.getPurchaseStatsByDate(request.serviceTypes, request.entityCode, yesterday)
        val todayCashFlow = todaySalesData.totalRevenue?.minus(todayPurchaseData.totalExpense ?: 0.toBigDecimal())
        val yesterdayCashFlow = yesterdaySalesData.totalRevenue?.minus(yesterdayPurchaseData.totalExpense ?: 0.toBigDecimal())
        val cashFlowChange = todayCashFlow?.minus(yesterdayCashFlow!!)
        val cashFlowChangePercentage = cashFlowChange?.let {
            yesterdayCashFlow?.takeIf { it != BigDecimal.ZERO }?.let {
                (cashFlowChange.divide(yesterdayCashFlow.abs(), 5, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100))
            }
        } ?: BigDecimal.ZERO

        response.totalCashFlow = todayCashFlow
        response.yesterdayCashFlow = yesterdayCashFlow
        response.cashFlowDiffFromYesterday = cashFlowChangePercentage
        return response
    }

    override suspend fun getFinanceShipmentProfit(request: BfProfitabilityReq): ShipmentProfitResp {

        var query: String? = null
        if (request.q != null) query = "%${request.q}%"
        var taggedEntityCode = mutableListOf<String>()
        request.entityCode?.forEach {
            taggedEntityCode.add(ENTITY_ID[it]!!)
        }
        val listResponse = unifiedDBRepo.listShipmentProfitability(
            request.pageIndex!!,
            request.pageSize!!,
            query,
            request.jobStatus,
            request.sortBy,
            request.sortType,
            taggedEntityCode,
            request.startDate,
            request.endDate,
            request.serviceTypes
        )
        listResponse.forEach {
            it.entity = TAGGED_ENTITY_ID_MAPPINGS[it.taggedEntityId].toString()
        }
        val totalRecords = unifiedDBRepo.findTotalCountShipment(
            query,
            request.jobStatus,
            taggedEntityCode,
            request.startDate,
            request.endDate,
            request.serviceTypes
        )
        return ShipmentProfitResp(
            shipmentList = listResponse,
            averageShipmentProfit = totalRecords.averageProfit,
            averageCustomerProfit = null,
            pageIndex = request.pageIndex,
            pageSize = request.pageSize,
            totalRecord = totalRecords.totalCount
        )
    }

    override suspend fun getFinanceCustomerProfit(request: BfProfitabilityReq): ShipmentProfitResp {
        var query: String? = null
        if (request.q != null) query = "%${request.q}%"
        val listResponse = unifiedDBRepo.listCustomerProfitability(
            request.pageIndex!!,
            request.pageSize!!,
            query,
            request.sortBy,
            request.sortType,
            request.entityCode
        )
        val totalRecords = unifiedDBRepo.findTotalCountCustomer(
            query,
            request.entityCode
        )
        return ShipmentProfitResp(
            customerList = listResponse,
            averageShipmentProfit = null,
            averageCustomerProfit = totalRecords.averageProfit,
            pageIndex = request.pageIndex,
            pageSize = request.pageSize,
            totalRecord = totalRecords.totalCount
        )
    }

    override suspend fun getFinanceServiceWiseRecPay(request: ServiceWiseRecPayReq): MutableList<ServiceWiseRecPayResp> {
        val response = mutableListOf<ServiceWiseRecPayResp>()
        val entityCode = request.entityCode
        val defaultersOrgIds = getDefaultersOrgIds()
        val oceanReceivable = unifiedDBRepo.getTotalRemainingAmountAR(AccMode.AR, listOf(AccountType.SREIMB, AccountType.SREIMBCN, AccountType.SCN, AccountType.SINV), OCEAN_SERVICES, entityCode, request.startDate, request.endDate, defaultersOrgIds)
        val oceanPayable = unifiedDBRepo.getTotalRemainingAmountAP(AccMode.AP, listOf(AccountType.PREIMB, AccountType.PCN, AccountType.PINV), OCEAN_SERVICES, entityCode, request.startDate, request.endDate)
        val airReceivable = unifiedDBRepo.getTotalRemainingAmountAR(AccMode.AR, listOf(AccountType.SREIMB, AccountType.SREIMBCN, AccountType.SCN, AccountType.SINV), AIR_SERVICES, entityCode, request.startDate, request.endDate, defaultersOrgIds)
        val airPayable = unifiedDBRepo.getTotalRemainingAmountAP(AccMode.AP, listOf(AccountType.PREIMB, AccountType.PCN, AccountType.PINV), AIR_SERVICES, entityCode, request.startDate, request.endDate)
        val surfaceReceivable = unifiedDBRepo.getTotalRemainingAmountAR(AccMode.AR, listOf(AccountType.SREIMB, AccountType.SREIMBCN, AccountType.SCN, AccountType.SINV), SURFACE_SERVICES, entityCode, request.startDate, request.endDate, defaultersOrgIds)
        val surfacePayable = unifiedDBRepo.getTotalRemainingAmountAP(AccMode.AP, listOf(AccountType.PREIMB, AccountType.PCN, AccountType.PINV), SURFACE_SERVICES, entityCode, request.startDate, request.endDate)

        response.add(
            ServiceWiseRecPayResp(
                service = "Ocean",
                accountPay = oceanPayable,
                accountRec = oceanReceivable
            )
        )
        response.add(
            ServiceWiseRecPayResp(
                service = "Air",
                accountPay = airPayable,
                accountRec = airReceivable
            )
        )
        response.add(
            ServiceWiseRecPayResp(
                service = "Surface",
                accountPay = surfacePayable,
                accountRec = surfaceReceivable
            )
        )
        return response
    }

    override suspend fun getFinanceServiceWiseOverdue(request: BfServiceWiseOverdueReq): ServiceWiseOverdueResp {
        val tradeTypes = when (request.tradeType) {
            "import" -> listOf("import", "IMPORT")
            "export" -> listOf("export", "EXPORT")
            "other" -> listOf("domestic", "DOMESTIC", "LOCAL", "local")
            "domestic" -> listOf("domestic", "DOMESTIC")
            "local" -> listOf("LOCAL", "local")
            else -> null
        }
        val defaultersOrgIds = getDefaultersOrgIds()
        return when (request.interfaceType) {
            "ocean" -> ServiceWiseOverdueResp(
                arData = getFinanceReceivableData(BfPendingAmountsReq(OCEAN_SERVICES, AccMode.AR, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                apData = getFinanceReceivableData(BfPendingAmountsReq(OCEAN_SERVICES, AccMode.AP, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                cardDataAr = unifiedDBRepo.getFinanceArCardData(OCEAN_SERVICES, request.startDate, request.endDate, request.entityCode, defaultersOrgIds),
                cardDataAp = unifiedDBRepo.getFinanceApCardDate(OCEAN_SERVICES, request.startDate, request.endDate, request.entityCode)
            )
            "air" -> ServiceWiseOverdueResp(
                arData = getFinanceReceivableData(BfPendingAmountsReq(AIR_SERVICES, AccMode.AR, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                apData = getFinanceReceivableData(BfPendingAmountsReq(AIR_SERVICES, AccMode.AP, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                cardDataAr = unifiedDBRepo.getFinanceArCardData(AIR_SERVICES, request.startDate, request.endDate, request.entityCode, defaultersOrgIds),
                cardDataAp = unifiedDBRepo.getFinanceApCardDate(AIR_SERVICES, request.startDate, request.endDate, request.entityCode)
            )
            "surface" -> ServiceWiseOverdueResp(
                arData = getFinanceReceivableData(BfPendingAmountsReq(SURFACE_SERVICES, AccMode.AR, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                apData = getFinanceReceivableData(BfPendingAmountsReq(SURFACE_SERVICES, AccMode.AP, null, request.startDate, request.endDate, tradeTypes, request.entityCode)),
                cardDataAr = unifiedDBRepo.getFinanceArCardData(SURFACE_SERVICES, request.startDate, request.endDate, request.entityCode, defaultersOrgIds),
                cardDataAp = unifiedDBRepo.getFinanceApCardDate(SURFACE_SERVICES, request.startDate, request.endDate, request.entityCode)
            )
            else -> throw AresException(AresError.ERR_1009, "interface type is invalid")
        }
    }

    override suspend fun getCustomersOverallStats(request: OverallStatsForCustomers): ResponseList<StatsForCustomerResponse?> {
        val responseList = ResponseList<StatsForCustomerResponse?>()
        responseList.list = accountUtilizationRepository.getOverallStatsForMultipleCustomers(request.bookingPartyIds)
        return responseList
    }

    private fun getCompanyType(companyType: CompanyType?): List<String>? {
        return when (companyType != null) {
            true -> when (companyType == CompanyType.IE) {
                true -> listOf(CompanyType.LONGTAIL.value, CompanyType.MIDSIZE.value)
                else -> listOf(companyType.value)
            }
            else -> null
        }
    }
}
