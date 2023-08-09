package com.cogoport

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.implementation.DashboardServiceImpl
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
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
        val endpoint = "/dashboard/daily-sales-outstanding?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
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
        val endpoint = "/dashboard/quarterly-outstanding?serviceType=FCL_FREIGHT&entityCode=301&year=2023&companyType=IE"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(unifiedDBRepository.generateQuarterlyOutstanding(any(), any(), any(), any(), any()))
            .thenReturn(dashboardHelper.getOutstanding())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(QuarterlyOutstanding::class.java)
            )
        }

        val expectedResult = dashboardHelper.getQuarterlyOutstanding()
        Assertions.assertEquals(expectedResult, response)
    }
}
