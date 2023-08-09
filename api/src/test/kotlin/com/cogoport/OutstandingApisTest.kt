package com.cogoport

import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.DefaultedBusinessPartnersServiceImpl
import com.cogoport.ares.api.payment.service.implementation.OpenSearchServiceImpl
import com.cogoport.ares.api.payment.service.implementation.OutStandingServiceImpl
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.brahma.opensearch.Client
import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
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
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class OutstandingApisTest(
    @InjectMocks
    val outStandingServiceImpl: OutStandingServiceImpl,
    @InjectMocks
    val defaultedBusinessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl,
    @InjectMocks
    val openSearchServiceImpl: OpenSearchServiceImpl
) {

    @Inject
    @field:io.micronaut.http.client.annotation.Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationHelper: AccountUtilizationHelper

    @Inject
    lateinit var outstandingHelper: OutstandingHelper

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepository.deleteAll()
        Client.deleteIndex(indexName = "index_ares_sales_outstanding")
    }

    @Test
    fun outstandingOverallTest() = runTest {
        val endPoint = "/outstanding/overall?orgId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        accountUtilizationHelper.saveAccountUtil()
        openSearchServiceImpl.pushOutstandingData(
            OpenSearchRequest(
                orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe",

            )
        )
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, OutstandingList::class.java)
        }
        val content = jsonMapper().readValue(javaClass.getResource("/fixtures/response/OutstandingList.json")!!.readText(), OutstandingList::class.java)
        Assertions.assertEquals(content, response.body())
    }

    @Test
    fun pushSalesOutstandingDataTest() = runTest {
        val endPoint = "/outstanding/open-search/add?orgId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        accountUtilizationHelper.saveAccountUtil()
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, Any::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }
}
