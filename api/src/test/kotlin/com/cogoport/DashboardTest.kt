package com.cogoport

import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.implementation.DashboardServiceImpl
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.collections.LinkedHashMap

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class DashboardTest(@InjectMocks val dashboardServiceImpl: DashboardServiceImpl) {
    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var settlementHelper: SettlementHelper

    @Mock
    lateinit var unifiedDBRepository: UnifiedDBRepo

    @Inject
    lateinit var dashboardHelper: DashboardHelper

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @Test
    fun canGetDailySalesOutstanding() = runTest {
        val endpoint =
            "/dashboard/daily-sales-outstanding?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(unifiedDBRepository.generateDailySalesOutstanding(any(), any(), any(), any(), any()))
            .thenReturn(dashboardHelper.getDailySalesOutstandingResponse())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(DailySalesOutstanding::class.java)
            )
        }

        val expectedResult = dashboardHelper.getDailyOutstanding()

        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canGetQuarterlySalesOutstanding() = runTest {
        val endpoint =
            "/dashboard/quarterly-outstanding?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(unifiedDBRepository.generateQuarterlyOutstanding(any(), any(), any(), any(), any()))
            .thenReturn(mutableListOf(Outstanding()))

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(QuarterlyOutstanding::class.java)
            )
        }

        val expectedResult = dashboardHelper.getQuarterlyOutstanding()
        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canGetOutstandingByAge() = runTest {
        val endpoint = "/dashboard/outstanding-by-age?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(unifiedDBRepository.getOutstandingByAge(any(), any(), any(), any()))
            .thenReturn(dashboardHelper.getOutstandingByAge())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request,
                Argument.of(LinkedHashMap::class.java, String::class.java, OverallAgeingStatsResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getOutstandingByAgeResponse()
        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canGetOverAllStats() = runTest {
        val accUtilDoc = settlementHelper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.SINV,
            223000,
            "SINV123455",
            123455,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(20),
            BigDecimal(20),
            BigDecimal(100),
            BigDecimal(100)
        )
        val endpoint = "/dashboard/kam/overall-stats"
        val request = HttpRequest.POST(
            endpoint,
            KamPaymentRequest(
                docValue = listOf(accUtilDoc.documentValue!!)
            )
        )

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(StatsForKamResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getKamWiseResponse()
        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canGetOverallStatsForCustomers() = runTest {
        val accUtilDoc = settlementHelper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.SINV,
            223000,
            "SINV123455",
            123455,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(20),
            BigDecimal(20),
            BigDecimal(100),
            BigDecimal(100)
        )
        val endpoint = "/dashboard/customer/overall-stats"
        val request = HttpRequest.POST(
            endpoint,
            CustomerStatsRequest(
                docValues = listOf(accUtilDoc.documentValue!!),
                bookingPartyId = "9f03db0c-88cc-450f-bbb1-38fa31861911",
                pageIndex = 1,
                pageSize = 10,
                sortType = "proforma_invoices_count",
                sortBy = "Asc"
            )
        )

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, StatsForCustomerResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getCustomerLevelOverallStats()
        Assertions.assertEquals(expectedResult, response?.list)
    }

    @Test
    fun canGetOverallStatsForTradeParties() = runTest {
        val accUtilDoc = settlementHelper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.SINV,
            223000,
            "SINV123455",
            123455,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(20),
            BigDecimal(20),
            BigDecimal(100),
            BigDecimal(100)
        )
        val endpoint = "/dashboard/trade-party/stats"
        val request = HttpRequest.POST(
            endpoint,
            TradePartyStatsRequest(
                docValues = listOf(accUtilDoc.documentValue!!),
                pageIndex = 1,
                pageSize = 10
            )
        )

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, OverallStatsForTradeParty::class.java)
            )
        }

        val expectedResult = dashboardHelper.getOverallStatsForTradeParty()
        Assertions.assertEquals(expectedResult, response?.list)
    }

    @Test
    fun canGetInvoiceListForTradeParties() = runTest {
        val accUtilDoc = settlementHelper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.SINV,
            223000,
            "SINV123455",
            123455,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(20),
            BigDecimal(20),
            BigDecimal(100),
            BigDecimal(100)
        )
        val endpoint = "/dashboard/trade-party/invoice/list"
        val request = HttpRequest.POST(
            endpoint,
            InvoiceListRequestForTradeParty(
                docValues = listOf(accUtilDoc.documentValue!!),
                pageIndex = 1,
                pageSize = 10,
                sortType = "invoice_amount",
                sortBy = "Asc"
            )
        )

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, InvoiceListResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getInvoiceListForTradeParties()
        Assertions.assertEquals(expectedResult, response?.list)
    }

    @Test
    fun canGetSalesFunnel() = runTest {
        val endpoint = "/dashboard/sales-funnel?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(SalesFunnelResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getSalesFunnelResponse()
        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canInvoiceTatStats() = runTest {
        val endpoint = "/dashboard/invoice-tat-stats?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(InvoiceTatStatsResponse::class.java)
            )
        }

        val expectedResult = dashboardHelper.getInvoiceTatStats()
        Assertions.assertEquals(expectedResult, response)
    }

    @Test
    fun canGetKamWiseOutstanding() = runTest {
        val endpoint = "/dashboard/kam-wise-outstanding?serviceType=FCL_FREIGHT&entityCode=301&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(unifiedDBRepository.getKamWiseOutstanding(any(), any(), any(), any(), any()))
            .thenReturn(dashboardHelper.getKamWiseOutstanding())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(List::class.java, KamWiseOutstanding::class.java)
            )
        }

        val expectedResult = dashboardHelper.getKamWiseOutstanding()

        Assertions.assertEquals(expectedResult, response)
    }
}
