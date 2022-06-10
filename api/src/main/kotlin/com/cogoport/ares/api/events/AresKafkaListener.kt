package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.KnockOffUtilizationEvent
import com.cogoport.ares.model.payment.event.CreateInvoiceEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusEvent
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

    @Topic("create-account-utilization")
    fun listenCreateAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        invoiceService.addAccountUtilization(accountUtilizationEvent.accUtilizationRequest)
    }

    @Topic("delete-invoice")
    fun deleteInvoice(data: MutableList<Pair<Long, String>>) = runBlocking {
        invoiceService.deleteInvoice(data)
    }

    @Topic("receivables-dashboard-data")
    fun listenDashboardData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushDashboardData(openSearchEvent.openSearchRequest)
    }

    @Topic("knockoff-payables")
    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
    }

    @Topic("update-invoice")
    fun listenUpdateInvoice(updateInvoiceEvent: UpdateInvoiceEvent) = runBlocking {
        invoiceService.updateInvoice(updateInvoiceEvent.updateInvoiceRequest)
    }

    @Topic("update-invoice-status")
    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
        invoiceService.updateInvoiceStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
    }

    @Topic("delete-create-invoice")
    fun listenDeleteCreateInvoice(createInvoiceEvent: CreateInvoiceEvent) = runBlocking {
        invoiceService.deleteCreateInvoice(createInvoiceEvent.createInvoiceRequest)
    }
}
