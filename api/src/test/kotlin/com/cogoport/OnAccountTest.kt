package com.cogoport

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.OnAccountServiceImpl
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
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

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class OnAccountTest (
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
    fun canGetPaymentList () = runTest {
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

        Assertions.assertEquals(expectedResult, response?.list)
    }
}