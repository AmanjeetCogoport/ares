package com.cogoport

import com.cogoport.ares.api.common.AresConstants.ENTITY_ID
import com.cogoport.ares.api.common.service.implementation.Scheduler
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.DefaultedBusinessPartnersServiceImpl
import com.cogoport.ares.api.payment.service.implementation.OpenSearchServiceImpl
import com.cogoport.ares.api.payment.service.implementation.OutStandingServiceImpl
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.net.URI
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class OutstandingApisTest(
    @InjectMocks
    val outStandingServiceImpl: OutStandingServiceImpl,
    @InjectMocks
    val defaultedBusinessPartnersServiceImpl: DefaultedBusinessPartnersServiceImpl,
    @InjectMocks
    val openSearchServiceImpl: OpenSearchServiceImpl,
    @InjectMocks
    val scheduler: Scheduler
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

    @Mock
    var emitter: AresMessagePublisher = Mockito.mock(AresMessagePublisher::class.java)

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepository.deleteAll()
        Client.createIndex("supplier_outstanding_overall")
        Client.createIndex(indexName = "index_ares_sales_outstanding")
        Client.createIndex(indexName = "index_ares_invoice_outstanding")
        ENTITY_ID.keys.map {
            Client.createIndex(indexName = "customer_outstanding_$it")
        }
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepository.deleteAll()
        Client.deleteIndex(indexName = "index_ares_sales_outstanding")
        Client.deleteIndex("supplier_outstanding_overall")
        Client.deleteIndex(indexName = "index_ares_invoice_outstanding")

        ENTITY_ID.keys.map {
            Client.deleteIndex(indexName = "customer_outstanding_$it")
        }
    }

    @Test
    fun outstandingOverallTest() = runTest {
        val endPoint = "/outstanding/overall?orgId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        accountUtilizationHelper.saveAccountUtil()
        openSearchServiceImpl.pushOutstandingData(
            OpenSearchRequest(
                orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe"
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

    @Test
    fun supplierOutstandingOverallTest() = runTest {
        val endPoint = "/outstanding/bill-overall?orgId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        accountUtilizationHelper.saveApAccountUtil()
        openSearchServiceImpl.pushOutstandingData(
            OpenSearchRequest(
                orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe"
            )
        )
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        val content = javaClass.getResource("/fixtures/response/SupplierOutstandingList.json")!!.readText()
        Assertions.assertEquals(content, response.body())
    }

    @Test
    fun createSupplierDetailsTest() = runTest {
        val endPoint = "/outstanding/supplier"
        val requestBody = outstandingHelper.getSupplierOutstandingDocument()
        val request = HttpRequest.POST<Any>(URI.create(endPoint), requestBody)
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun listSupplierOutstandingOverallTest() = runTest {
        val endPoint = "/outstanding/by-supplier"
        val supplierDetails = outstandingHelper.getSupplierOutstandingDocument()
        outStandingServiceImpl.createSupplierDetails(supplierDetails)
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        val content = javaClass.getResource("/fixtures/response/ListSupplierOutstandingOverall.json")!!.readText()
        Assertions.assertEquals(content, response.body())
    }

    @Test
    fun listOutstandingInvoicesTest() = runTest {
        val endPoint = "/outstanding/invoice-list?orgId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        outstandingHelper.saveCustomerInvoiceResponseDoc()
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, ListInvoiceResponse::class.java)
        }
        val content = jsonMapper().readValue(javaClass.getResource("/fixtures/response/ListInvoiceResponse.json")!!.readText(), ListInvoiceResponse::class.java)
        Assertions.assertEquals(content, response.body())
    }

    @Test
    fun getCurrOutstandingTest() = runTest {
        val endPoint = "/outstanding/outstanding-days"
        accountUtilizationHelper.saveAccountUtil()
        val request = HttpRequest.POST<Any>(URI.create(endPoint), listOf(113121115))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals("0", response.body())
    }

    @Test
    fun getCustomerOutstandingInInrTest() = runTest {
        val endPoint = "/outstanding/customer-outstanding"
        accountUtilizationHelper.saveAccountUtil()
        openSearchServiceImpl.pushOutstandingData(
            OpenSearchRequest(
                orgId = "9b92503b-6374-4274-9be4-e83a42fc35fe"
            )
        )
        val request = HttpRequest.POST<Any>(URI.create(endPoint), listOf("9b92503b-6374-4274-9be4-e83a42fc35fe"))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        val expected = hashMapOf("9b92503b-6374-4274-9be4-e83a42fc35fe" to 400.toBigDecimal().setScale(4))
        Assertions.assertEquals(jsonMapper().writeValueAsString(expected), response.body())
    }

    @Test
    fun updateSupplierDetailsTest() = runTest {
        val endPoint = "/outstanding/supplier"
        accountUtilizationHelper.saveApAccountUtil()
        val requestBody = outstandingHelper.getSupplierOutstandingDocument()
        outStandingServiceImpl.createSupplierDetails(requestBody)
        val request = HttpRequest.PUT<Any>(
            URI.create(endPoint),
            UpdateSupplierOutstandingRequest(
                orgId = UUID.fromString("9b92503b-6374-4274-9be4-e83a42fc35fe")
            )
        )
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun migrateSupplierOutstanding() = runTest {
        val endPoint = "/outstanding/supplier-outstanding-migrate"
        accountUtilizationHelper.saveApAccountUtil()
        val request = HttpRequest.PUT<Any>(
            URI.create(endPoint),
            null
        )
        whenever(emitter.emitUpdateSupplierOutstanding(any())).thenReturn(Unit)
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun createCustomerDetailsTest() = runTest {
        val endPoint = "/outstanding/customer"
        accountUtilizationHelper.saveAccountUtil()
        val request = HttpRequest.POST<Any>(
            URI.create(endPoint),
            outstandingHelper.getCustomerOutstandingDocument()
        )
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun updateCustomerDetailsTest() = runTest {
        val endPoint = "/outstanding/customer"
        accountUtilizationHelper.saveAccountUtil()
        val requestBody = outstandingHelper.getCustomerOutstandingDocument()
        outStandingServiceImpl.createCustomerDetails(requestBody)
        val request = HttpRequest.PUT<Any>(
            URI.create(endPoint),
            UpdateSupplierOutstandingRequest(
                orgId = UUID.fromString("9b92503b-6374-4274-9be4-e83a42fc35fe")
            )
        )
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun listCustomerOutstandingOverallTest() = runTest {
        val endPoint = "/outstanding/by-customer?tradePartyDetailId=9b92503b-6374-4274-9be4-e83a42fc35fe"
        accountUtilizationHelper.saveAccountUtil()
        val customerDetails = outstandingHelper.getCustomerOutstandingDocument()
        outStandingServiceImpl.createCustomerDetails(customerDetails)
        val request = HttpRequest.GET<Any>(URI.create(endPoint))
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        val content = javaClass.getResource("/fixtures/response/ListCustomerOutstandingOverall.json")!!.readText()
        Assertions.assertEquals(content, response.body())
    }

    @Test
    fun migrateCustomerOutstanding() = runTest {
        val endPoint = "/outstanding/customer-outstanding-migrate"
        accountUtilizationHelper.saveAccountUtil()
        val request = HttpRequest.PUT<Any>(
            URI.create(endPoint),
            null
        )
        whenever(emitter.emitUpdateCustomerOutstanding(any())).thenReturn(Unit)
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().exchange(request, String::class.java)
        }
        Assertions.assertEquals(HttpStatus.OK, response.status)
    }
}
