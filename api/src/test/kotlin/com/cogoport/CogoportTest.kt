package com.cogoport

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.UnifiedDBNewRepository
import com.cogoport.ares.api.payment.service.implementation.OutStandingServiceImpl
import com.cogoport.ares.api.reports.services.implementation.ReportServiceImpl
import com.cogoport.brahma.s3.client.S3Client
import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
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
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class CogoportTest(
    @InjectMocks
    val outStandingServiceImpl: OutStandingServiceImpl,
    @InjectMocks
    val reportServiceImpl: ReportServiceImpl
) {

    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Mock
    var s3Client: S3Client = Mockito.mock(S3Client::class.java)

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Mock
    var unifiedDBNewRepository: UnifiedDBNewRepository = mock(UnifiedDBNewRepository::class.java)

    @Inject
    lateinit var accountUtilizationHelper: AccountUtilizationHelper

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    // @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @Test
    fun arLedgerReport() = runTest {
        val endPoint = "/report/ar-ledger?"
        val startDate = LocalDate.of(2023, 1, 1)
        val endDate = LocalDate.of(2023, 1, 31)
        val orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe"
        val orgName = "SUN PHARMACEUTICAL INDUSTRIES"

        val excelUrl = "https://business-finance-test.s3.ap-south-1.amazonaws.com/AR_Ledger_Report_${orgName.replace(" ", "_")}_from_${startDate}_to_$endDate.xlsx"
        whenever(s3Client.upload(any(), any(), any())).thenReturn(URL(excelUrl))
        val req = "orgId=$orgId&startDate=$startDate&endDate=$endDate&orgName=SUN%20PHARMACEUTICAL%20INDUSTRIES&requestedBy=${UUID.randomUUID()}&entityCodes=301"
        val request = HttpRequest.GET<Any>(URI.create(endPoint + req))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(request, Argument.of(String::class.java))
        }
        Assertions.assertEquals(excelUrl, response)
    }

    @Test
    fun getOrganizationTradePartyOutstandingTest() = runTest {
        val endPoint = "/outstanding/trade-party-outstanding"
        val orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe"
        val req = "?orgIds=$orgId"
        whenever(unifiedDBNewRepository.getTradePartyOutstanding(any(), any())).thenReturn(accountUtilizationHelper.getOrganizationTradePartyOutstandingResponse())

        val request = HttpRequest.GET<Any>(URI.create(endPoint + req))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(request, Argument.of(String::class.java))
        }
        Assertions.assertEquals(jsonMapper().writeValueAsString(accountUtilizationHelper.getOrganizationTradePartyOutstandingResponse()), response)
    }
}
