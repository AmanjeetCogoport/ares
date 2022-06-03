package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.KnockOffUtilizationEvent
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.Topic
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

@KafkaListener(offsetReset = OffsetReset.EARLIEST)
class AresKafkaListener {
    @Inject
    private lateinit var invoiceService: InvoiceService

    @Inject
    private lateinit var openSearchService: OpenSearchService

    @Inject
    lateinit var knockoffService: KnockoffService
    @Topic("account-utilization")
    fun listenAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        invoiceService.addAccountUtilization(accountUtilizationEvent.accUtilizationRequest)
    }

    @Topic("receivables-dashboard-data")
    fun listenDashboardData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushDashboardData(openSearchEvent.openSearchRequest)
    }

    @Topic("knockoff-payables")
    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
    }
}
