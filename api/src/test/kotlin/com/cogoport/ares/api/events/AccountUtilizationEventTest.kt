package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import java.sql.Timestamp
import java.util.UUID

@MicronautTest
internal class AccountUtilizationEventTest {

    @Inject
    lateinit var emitter: AccountUtilizationEmitter

    @Inject
    lateinit var invoiceService: InvoiceService

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @OptIn(ExperimentalCoroutinesApi::class)
    // @Test
    fun testPubSubWorks() = runTest {

        assertTrue(application.isRunning)

        emitter.emitAccountUtilizationEvent(
            accountUtilizationEvent = AccountUtilizationEvent(
                accUtilizationRequest = AccUtilizationRequest(
                    documentNo = 123,
                    entityCode = 1,
                    entityId = "123",
                    organizationId = UUID(10, 10),
                    orgSerialId = 10,
                    sageOrganizationId = "101",
                    organizationName = "Test",
                    accCode = 1,
                    accType = AccountType.PINV.toString(),
                    accMode = "ar",
                    signFlag = 1,
                    currencyAmount = 100.toBigDecimal(),
                    ledgerAmount = 100.toBigDecimal(),
                    currencyPayment = 100.toBigDecimal(),
                    ledgerPayment = 100.toBigDecimal(),
                    zoneCode = "North",
                    docStatus = "Proforma",
                    docValue = "IDK",
                    dueDate = Timestamp.valueOf("2022-01-01"),
                    transactionDate = null
                )
            )
        )

        val deferred = async {
            withContext(Dispatchers.Default) {
                delay(5_000) // Dispatchers.Default doesn't know about TestCoroutineScheduler

                val accUtilization = invoiceService.findByDocumentNo(123)

                assertNotNull(accUtilization)
                assertNotNull(accUtilization.id)
                assertEquals(accUtilization.accCode, 1)
            }
        }
        deferred.await()
    }
}
