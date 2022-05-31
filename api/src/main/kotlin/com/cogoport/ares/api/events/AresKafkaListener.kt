package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.Topic
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

@KafkaListener(offsetReset = OffsetReset.EARLIEST)
class AresKafkaListener {
    @Inject
    private lateinit var invoiceService: InvoiceService

    @Topic("account-utilization")
    fun listenAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        invoiceService.accountUtilization(accountUtilizationEvent.accUtilizationRequest)
    }

    @Topic("receivables-dashboard-data")
    fun listenDashboardData(accountUtilizationEvent: AccountUtilizationEvent): Nothing = runBlocking {
        TODO("Add Dashboard Doc update service call here")
    }
}
