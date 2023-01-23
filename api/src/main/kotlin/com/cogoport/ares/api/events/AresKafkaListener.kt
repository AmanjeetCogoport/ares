package com.cogoport.ares.api.events

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.context.annotation.Property
import org.apache.kafka.clients.consumer.ConsumerConfig

@KafkaListener(
    offsetReset = OffsetReset.EARLIEST, pollTimeout = "10000ms",
    offsetStrategy = OffsetStrategy.SYNC_PER_RECORD, threads = 12, heartbeatInterval = "1000ms",
    properties = [
        Property(name = ConsumerConfig.MAX_POLL_RECORDS_CONFIG, value = "10"),
        Property(name = ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, value = "20000"),
        Property(name = ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, value = "org.apache.kafka.clients.consumer.RoundRobinAssignor")
    ]
)
class AresKafkaListener {
//    @Inject
//    private lateinit var accountUtilService: AccountUtilizationService
//
//    @Inject
//    private lateinit var openSearchService: OpenSearchService
//
//    @Inject
//    private lateinit var knockoffService: KnockoffService
//
//    @Inject
//    private lateinit var settlementService: SettlementService
//
//    @Inject
//    private lateinit var outstandingService: OutStandingService
//
//    /*For Saving  both Account Payables and Account Receivables bills/invoices amount */
//    @Topic("create-account-utilization")
//    fun listenCreateAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
//        accountUtilService.add(accountUtilizationEvent.accUtilizationRequest)
//    }
//
//    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
//    @Topic("update-account-utilization")
//    fun listenUpdateAccountUtilization(updateInvoiceEvent: UpdateInvoiceEvent) = runBlocking {
//        accountUtilService.update(updateInvoiceEvent.updateInvoiceRequest)
//    }
//
//    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
//    @Topic("update-account-status")
//    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
//        accountUtilService.updateStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
//    }
//
//    /*For deleting the invoices*/
//    @Topic("delete-account-utilization")
//    fun listenDeleteAccountUtilization(deleteInvoiceEvent: DeleteInvoiceEvent) = runBlocking {
//        accountUtilService.delete(deleteInvoiceEvent.deleteInvoiceRequest)
//    }
//
//    @Topic("receivables-dashboard-data")
//    fun listenDashboardData(openSearchEvent: OpenSearchEvent) = runBlocking {
//        openSearchService.pushDashboardData(openSearchEvent.openSearchRequest)
//    }
//
//    @Topic("receivables-outstanding-data")
//    fun listenOutstandingData(openSearchEvent: OpenSearchEvent) = runBlocking {
//        openSearchService.pushOutstandingData(openSearchEvent.openSearchRequest)
//    }
//
//    @Topic("knockoff-payables")
//    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
//        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
//    }
//
//    @Topic("reverse-utr")
//    fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) = runBlocking {
//        knockoffService.reverseUtr(reverseUtrRequest)
//    }
//
//    @Topic("send-payment-details-for-autoKnockOff")
//    fun settleWithSourceIdAndDestinationId(autoKnockOffRequest: AutoKnockOffRequest) = runBlocking {
//        settlementService.settleWithSourceIdAndDestinationId(autoKnockOffRequest)
//    }
//
//    @Topic("update-supplier-details")
//    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
//        outstandingService.updateSupplierDetails(request.orgId.toString(), false, null)
//    }
//
//    @Topic("unfreeze-credit-consumpation")
//    suspend fun unfreezeCreditConsumption(request: Settlement) = run { settlementService.sendKnockOffDataToCreditConsumption(request) }
}
