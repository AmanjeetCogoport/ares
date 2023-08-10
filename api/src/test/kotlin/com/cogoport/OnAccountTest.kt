package com.cogoport

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.CogoBackLowLevelClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.OnAccountServiceImpl
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.OrgStatsResponseForCoeFinance
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.PaymentResponse
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
class OnAccountTest(
    @InjectMocks val onAccountServiceImpl: OnAccountServiceImpl
) {
    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var paymentRepo: PaymentRepository

    @Inject
    lateinit var settlementHelper: SettlementHelper

    @Inject
    lateinit var onAccountHelper: OnAccountHelper

    @Mock
    var cogoClient: AuthClient = Mockito.mock(AuthClient::class.java)

    @Mock
    var cogoBackLowLevelClient: CogoBackLowLevelClient = Mockito.mock(CogoBackLowLevelClient::class.java)

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
        paymentRepo.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
        paymentRepo.deleteAll()
    }

    @Test
    fun canGetPaymentList() = runTest {
        val endpoint = "/accounts?page=1&pageLimit=10&entityType=301&sortBy=createdAt&sortType=Desc"
        val payment = settlementHelper.savePayment(
            321000,
            AccMode.AP,
            100.toBigDecimal(),
            100.toBigDecimal(),
            "INR",
            "INR",
            301,
            1.toBigDecimal(),
            paymentCode = PaymentCode.PAY,
            paymentNumValue = "PAY12345",
            paymentNum = 12345,
            signFlag = 1,
            transRefNumber = "abhishek_boss"
        )
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(cogoClient.getUsers(any())).thenReturn(onAccountHelper.getUserResponse())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(AccountCollectionResponse::class.java)
            )
        }

        val expectedResult = onAccountHelper.getAccountReceivables(payment)

        expectedResult.first()?.transactionDate = (response.list as List<PaymentResponse>).first().transactionDate
        expectedResult.first()?.createdAt = (response.list as List<PaymentResponse>).first().createdAt

        Assertions.assertEquals(expectedResult, response?.list)
    }

    @Test
    fun canGetOrgStats() = runTest {
        settlementHelper.saveAccountUtilizations(
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

        val endpoint = "/accounts/org-stats?orgId=9f03db0c-88cc-450f-bbb1-38fa31861911"
        val request = HttpRequest.GET<Any>(endpoint)

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(OrgStatsResponse::class.java)
            )
        }

        Assertions.assertEquals(onAccountHelper.getOrgStatsResponse(), response)
    }

    @Test
    fun canGetOrgStatsForCoeFinance() = runTest {
        settlementHelper.saveAccountUtilizations(
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

        val endpoint = "/accounts/org-stats-for-coe-finance?orgId=9f03db0c-88cc-450f-bbb1-38fa31861911"
        val request = HttpRequest.GET<Any>(endpoint)

        whenever(cogoBackLowLevelClient.getTradePartyOutstanding(any(), any())).thenReturn(onAccountHelper.getTradePartyOutstandingList())

        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(OrgStatsResponseForCoeFinance::class.java)
            )
        }

        Assertions.assertEquals(onAccountHelper.getOrgStatsResponseForCoeFinance(), response)
    }

    @Test
    fun canGetOnAccountTotalAmount() = runTest {
        settlementHelper.saveAccountUtilizations(
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

        val endpoint = "/accounts/on-account-payment?orgIdList=9f03db0c-88cc-450f-bbb1-38fa31861911&accType=REC&accMode=AR"
        val request = HttpRequest.GET<Any>(endpoint)
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(OnAccountTotalAmountResponse::class.java)
            )
        }
        Assertions.assertEquals(onAccountHelper.getOnAccountTotalAmountResponse(), response)
    }
}
