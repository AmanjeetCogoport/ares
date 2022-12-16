package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.model.payment.RestoreUtrResponse
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.kuber.model.bills.request.UpdatePaymentStatusRequest
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

    @Topic("update-bill-payment-status")
    fun emitUpdateBillPaymentStatus(updatePaymentStatusRequest: UpdatePaymentStatusRequest)

    @Topic("update-bill-archive")
    fun emitUpdateBillsToArchive(billId: Long)

    @Topic("update-invoice-archive")
    fun emitUpdateInvoicesToArchive(invoiceId: Long)

    @Topic("post-restore-utr")
    fun emitPostRestoreUtr(restoreUtrResponse: RestoreUtrResponse)
    @Topic("settlement-migration")
    fun emitSettlementRecord(settlementRecord: SettlementRecord)

    @Topic("update-utilization-amount")
    fun emitUtilizationUpdateRecord(paymentRecord: PaymentRecord)
}
