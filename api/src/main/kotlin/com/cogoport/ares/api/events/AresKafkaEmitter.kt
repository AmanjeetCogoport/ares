package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic

@KafkaClient
interface AresKafkaEmitter {

    @Topic("receivables-dashboard-data")
    fun emitDashboardData(openSearchEvent: OpenSearchEvent)

    @Topic("receivables-outstanding-data")
    fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Topic("payables-bill-status")
    fun emitBillPaymentStatus(payableProduceEvent: PayableKnockOffProduceEvent)

    @Topic("update-invoice-balance")
    fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent)

    @Topic("sage-payment-migration")
    fun emitPaymentMigration(paymentRecord: PaymentRecord)

    @Topic("sage-jv-migration")
    fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord)
}
