package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.KnockOffUtilizationEvent
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
    private lateinit var accountUtilService: AccountUtilizationService

    @Inject
    private lateinit var openSearchService: OpenSearchService

    @Inject
    private lateinit var knockoffService: KnockoffService

    /*For Saving  both Account Payables and Account Receivables bills/invoices amount */
    @Topic("create-account-utilization")
    fun listenCreateAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        accountUtilService.add(accountUtilizationEvent.accUtilizationRequest)
    }

    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
    @Topic("update-account-utilization")
    fun listenUpdateAccountUtilization(updateInvoiceEvent: UpdateInvoiceEvent) = runBlocking {
        accountUtilService.update(updateInvoiceEvent.updateInvoiceRequest)
    }

    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
    @Topic("update-invoice-status")
    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
        accountUtilService.updateStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
    }

    /*For deleting the invoices*/
    @Topic("delete-account-utilization")
    fun listenDeleteAccountUtilization(data: MutableList<Pair<Long, String>>) = runBlocking {
        accountUtilService.delete(data)
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
