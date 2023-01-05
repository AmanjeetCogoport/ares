package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.event.DeleteInvoiceEvent
import com.cogoport.ares.model.payment.event.KnockOffUtilizationEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import java.util.UUID

@KafkaListener(offsetReset = OffsetReset.LATEST, pollTimeout = "300000ms", offsetStrategy = OffsetStrategy.SYNC_PER_RECORD, threads = 2, heartbeatInterval = "1000ms")
class AresKafkaListener {
    @Inject
    private lateinit var accountUtilService: AccountUtilizationService

    @Inject
    private lateinit var openSearchService: OpenSearchService

    @Inject
    private lateinit var knockoffService: KnockoffService

    @Inject
    private lateinit var settlementService: SettlementService

    @Inject
    private lateinit var outStandingService: OutStandingService

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
    @Topic("update-account-status")
    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
        accountUtilService.updateStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
    }

    /*For deleting the invoices*/
    @Topic("delete-account-utilization")
    fun listenDeleteAccountUtilization(deleteInvoiceEvent: DeleteInvoiceEvent) = runBlocking {
        accountUtilService.delete(deleteInvoiceEvent.deleteInvoiceRequest)
    }

    @Topic("receivables-dashboard-data")
    fun listenDashboardData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushDashboardData(openSearchEvent.openSearchRequest)
    }

    @Topic("receivables-outstanding-data")
    fun listenOutstandingData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushOutstandingData(openSearchEvent.openSearchRequest)
    }

    @Topic("knockoff-payables")
    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
    }

    @Topic("reverse-utr")
    fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) = runBlocking {
        knockoffService.reverseUtr(reverseUtrRequest)
    }

    @Topic("send-payment-details-for-autoKnockOff")
    fun settleWithSourceIdAndDestinationId(autoKnockOffRequest: AutoKnockOffRequest) = runBlocking {
        settlementService.settleWithSourceIdAndDestinationId(autoKnockOffRequest)
    }

    @Topic("update-supplier-details")
    fun updateSupplierDetail(orgId: UUID?) = runBlocking {
        outStandingService.updateSupplierOutstanding(orgId.toString())
    }
}
