package com.cogoport

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.plutus.model.common.ResponseList
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class SettlementTest() {
    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var helper: SettlementHelper

    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
    }

    @Test
    fun settle() = runTest {
        val accUtil = helper.saveAccountUtilizations(
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
        logger().info("saved document id: $accUtil")

        val settlement = helper.saveSettlement(
            amount = BigDecimal(100),
            ledAmount = BigDecimal(100),
            currency = "INR",
            ledCurrency = "INR",
            destinationType = SettlementType.SINV,
            destinationId = 122334,
            sourceType = SettlementType.REC,
            sourceId = 1000000,
            settlementNum = "SETL12345",
            signFlag = 1
        )

        logger().info("settlement with id was saved :$settlement")
        val payment = helper.savePayment(
            321000,
            AccMode.AP,
            amount = BigDecimal(100),
            ledAmount = BigDecimal(100),
            currency = "INR",
            ledCurrency = "INR",
            entityCode = 301,
            exchangeRate = BigDecimal(1),
            paymentCode = PaymentCode.PAY,
            paymentNum = 5645328567,
            paymentNumValue = "PAY564532845",
            signFlag = 1,
            transRefNumber = "Payment Against VIvek garg"
        )

        logger().info("payment with id was saved :$payment")

        val jvData = helper.saveParentJournalVoucher(
            "JV/2324/1233",
            "MISC",
            "INR",
            "INR",
            "PAYMENT AGAINST 12345678",
            301,
            BigDecimal(1),
            BigDecimal(100),
            BigDecimal(100),
        )

        logger().info("parent jv: $jvData")
    }

    @Test
    fun getDocumentList () = runTest {
        val expectedResult = helper.saveAccountUtilizations(
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
        val req = "orgId=9f03db0c-88cc-450f-bbb1-38fa31861911&" + "accModes=AP&entityCode=301"
        val request = HttpRequest.GET<Any>(endpoint + req)
        val response = withContext(Dispatchers.IO) {
            client.toBlocking().retrieve(
                request, Argument.of(ResponseList::class.java)
            )
        }

        logger().info(response.toString())
    }
}
