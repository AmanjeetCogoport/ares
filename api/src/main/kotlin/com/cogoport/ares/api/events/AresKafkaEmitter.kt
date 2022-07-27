package com.cogoport.ares.api.events

import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.settlement.UpdateInvoiceBalanceEvent
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic

@KafkaClient
interface AresKafkaEmitter {

    @Topic("account-utilization")
    fun emitAccountUtilizationEvent(accountUtilizationEvent: AccountUtilizationEvent)

    @Topic("receivables-dashboard-data")
    fun emitDashboardData(openSearchEvent: OpenSearchEvent)

    @Topic("receivables-outstanding-data")
    fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Topic("payables-bill-status")
    fun emitBillPaymentStatus(payableProduceEvent: PayableKnockOffProduceEvent)

    @Topic("update-invoice-balance")
    fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent)
}
