package com.cogoport

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.settlement.entity.SettlementListDoc
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.implementation.CpSettlementServiceImpl
import com.cogoport.ares.api.settlement.service.implementation.SettlementServiceHelper
import com.cogoport.ares.api.settlement.service.implementation.SettlementServiceImpl
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.kuber.client.KuberClient
import com.cogoport.plutus.client.PlutusClient
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class SettlementTest(
    @InjectMocks val settlementServiceImpl: SettlementServiceImpl,
    @InjectMocks val cpSettlementServiceImpl: CpSettlementServiceImpl,
    @InjectMocks val settlementServiceHelper: SettlementServiceHelper
) {
    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var settlementRepo: SettlementRepository

    @Inject
    lateinit var helper: SettlementHelper

    @Mock
    var plutusClient: PlutusClient = Mockito.mock(PlutusClient::class.java)

    @Mock
    var cogoClient: AuthClient = Mockito.mock(AuthClient::class.java)

    @Mock
    var kuberClient: KuberClient = Mockito.mock(KuberClient::class.java)

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
        settlementRepo.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
        settlementRepo.deleteAll()
    }
    @Test
    fun canGetSettlementList() = runTest {
        val endpoint = "/settlement/list?page=1&pageLimit=10&orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&accountType=All&entityCode=301&sortBy=settlementDate&sortType=Desc"
        val request = HttpRequest.GET<Any>(endpoint)

        val sourceDocument = helper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.REC,
            223000,
            "REC123456",
            123456,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(0),
            BigDecimal(0),
            BigDecimal(100),
            BigDecimal(100)
        )
        val destinationDocument = helper.saveAccountUtilizations(
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

        val settlementData = helper.saveSettlement(
            amount = BigDecimal(100),
            ledAmount = BigDecimal(100),
            currency = "INR",
            ledCurrency = "INR",
            destinationType = SettlementType.SINV,
            destinationId = destinationDocument.documentNo,
            sourceType = SettlementType.REC,
            sourceId = sourceDocument.documentNo,
            settlementNum = "SETL1234578",
            signFlag = 1,
            settlementDate = sourceDocument.transactionDate!!
        )
        whenever(plutusClient.getInvoiceAdditionalList(any(), any())).thenReturn(helper.getInvoiceAdditionalResponse())
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, SettlementListDoc::class.java)
            )
        }
        val expectedResult = helper.getSettlementList(settlementData)
        Assertions.assertEquals(expectedResult, response?.list)
    }

    @Test
    fun canGetAccountBalance() = runTest {
        helper.saveAccountUtilizations(
            AccMode.AP,
            AccountType.PINV,
            321000,
            "VIVEK/12234",
            12345,
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
        val endpoint = "/settlement/account-balance?"
        val req = "orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&" + "accModes=AP&entityCode=301"
        val request = HttpRequest.GET<Any>(endpoint + req)

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(SummaryResponse::class.java)
            )
        }

        Assertions.assertEquals(helper.getAccountBalance(), response)
    }
    @Test
    fun canGetTdsDocument() = runTest {
        val savedRecord = helper.saveAccountUtilizations(
            AccMode.AP,
            AccountType.PINV,
            321000,
            "VIVEK/12234",
            12345,
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
        val endpoint = "/settlement/tds/documents?"
        val req = "orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&" + "accMode=AP&sortBy=transactionDate&sortType=Desc"
        val request = HttpRequest.GET<Any>(endpoint + req)

        whenever(cogoClient.listOrgTdsStyles(any())).thenReturn(helper.getTdsResponse())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, Document::class.java)
            )
        }
        val expectedResult = helper.getDocumentListResponse(savedRecord, true)
        Assertions.assertEquals(expectedResult, response.list)
    }
    @Test
    fun canGetDocumentList() = runTest {
        val savedRecord = helper.saveAccountUtilizations(
            AccMode.AP,
            AccountType.PINV,
            321000,
            "VIVEK/12234",
            12345,
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
        val endpoint = "/settlement/documents?"
        val req = "orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&" + "accModes=AP&entityCode=301&" + "page=1&pageLimit=10"
        val request = HttpRequest.GET<Any>(endpoint + req)

        whenever(cogoClient.listOrgTdsStyles(any())).thenReturn(helper.getTdsResponse())
        whenever(kuberClient.billListByIds(any())).thenReturn(helper.getBillResponse())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, Document::class.java)
            )
        }

        val expectedResult = helper.getDocumentListResponse(savedRecord, false)
        Assertions.assertEquals(expectedResult, response.list)
    }

    @Test
    fun canGetInvoices() = runTest {
        val sourceDocument = helper.saveAccountUtilizations(
            AccMode.AR,
            AccountType.REC,
            223000,
            "REC123456",
            123456,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(0),
            BigDecimal(0),
            BigDecimal(100),
            BigDecimal(100)
        )
        val destinationDocument = helper.saveAccountUtilizations(
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

        val settlementData = helper.saveSettlement(
            amount = BigDecimal(100),
            ledAmount = BigDecimal(100),
            currency = "INR",
            ledCurrency = "INR",
            destinationType = SettlementType.SINV,
            destinationId = destinationDocument.documentNo,
            sourceType = SettlementType.REC,
            sourceId = sourceDocument.documentNo,
            settlementNum = "SETL1234578",
            signFlag = 1,
            settlementDate = sourceDocument.transactionDate!!
        )
        val endpoint = "/settlement/invoices?"
        val req = "orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&" + "entityCode=301&" + "page=1&pageLimit=10&status=PARTIAL_PAID&&accType=SINV"
        val request = HttpRequest.GET<Any>(endpoint + req)

        whenever(cogoClient.getOrgTdsStyles(any())).thenReturn(helper.getTdsDataResponse())
        whenever(plutusClient.getSidsForInvoiceIds(any())).thenReturn(helper.getSidResponse())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java, SettlementInvoiceResponse::class.java)
            )
        }

        val expectedResult = helper.getInvoiceResponse(settlementData, destinationDocument)
        Assertions.assertEquals(ObjectMapper().writeValueAsString(expectedResult), ObjectMapper().writeValueAsString(response.list))
    }
}
