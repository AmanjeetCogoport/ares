package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaidUnpaidStatus
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.payment.RestoreUtrResponse
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.kuber.model.bills.request.UpdatePaymentStatusRequest
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.tracing.annotation.NewSpan

@KafkaClient
interface AresKafkaEmitter {

    @NewSpan
    @Topic("receivables-dashboard-data")
    fun emitDashboardData(openSearchEvent: OpenSearchEvent)

    @NewSpan
    @Topic("receivables-outstanding-data")
    fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @NewSpan
    @Topic("payables-bill-status")
    fun emitBillPaymentStatus(payableProduceEvent: PayableKnockOffProduceEvent)

    @NewSpan
    @Topic("update-invoice-balance")
    fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent)

    @NewSpan
    @Topic("sage-payment-migration")
    fun emitPaymentMigration(paymentRecord: PaymentRecord)

    @NewSpan
    @Topic("sage-jv-migration")
    fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord)

    @NewSpan
    @Topic("update-bill-payment-status")
    fun emitUpdateBillPaymentStatus(updatePaymentStatusRequest: UpdatePaymentStatusRequest)

    @NewSpan
    @Topic("update-bill-archive")
    fun emitUpdateBillsToArchive(billId: Long)

    @NewSpan
    @Topic("update-invoice-archive")
    fun emitUpdateInvoicesToArchive(invoiceId: Long)

    @NewSpan
    @Topic("post-restore-utr")
    fun emitPostRestoreUtr(restoreUtrResponse: RestoreUtrResponse) // already in rab

    @NewSpan
    @Topic("settlement-migration")
    fun emitSettlementRecord(settlementRecord: SettlementRecord)

    @NewSpan
    @Topic("update-utilization-amount")
    fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest)

    @NewSpan
    @Topic("update-invoice-status-migration")
    fun emitInvoiceStatus(paidUnpaidStatus: PaidUnpaidStatus)

    @NewSpan
    @Topic("update-bill-status-migration")
    fun emitBIllStatus(paidUnpaidStatus: PaidUnpaidStatus)

    @NewSpan
    @Topic("unfreeze-credit-consumpation")
    fun emitUnfreezeCreditConsumption(request: Settlement) // moved to rabbitMQ

    @NewSpan
    @Topic("update-supplier-details")
    fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) // moved to rabbitMQ
}
