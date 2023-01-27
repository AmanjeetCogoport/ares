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
    fun emitDashboardData(openSearchEvent: OpenSearchEvent) // moved to rabbit mq

    @NewSpan
    @Topic("receivables-outstanding-data")
    fun emitOutstandingData(openSearchEvent: OpenSearchEvent) // moved to rabbit MQ

    @NewSpan
    @Topic("payables-bill-status")
    fun emitBillPaymentStatus(payableProduceEvent: PayableKnockOffProduceEvent) // moved to rabbit MQ

    @NewSpan
    @Topic("update-invoice-balance")
    fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent) // moved to rabbitMQ

    @NewSpan
    @Topic("sage-payment-migration")
    fun emitPaymentMigration(paymentRecord: PaymentRecord) // moved to rabbit mq

    @NewSpan
    @Topic("sage-jv-migration")
    fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord) // moved to rabbit mq

    @NewSpan
    @Topic("update-bill-payment-status")
    fun emitUpdateBillPaymentStatus(updatePaymentStatusRequest: UpdatePaymentStatusRequest) // moved to rabbit MQ

    @NewSpan
    @Topic("update-bill-archive")
    fun emitUpdateBillsToArchive(billId: Long) // moved to rabbit mq

    @NewSpan
    @Topic("update-invoice-archive")
    fun emitUpdateInvoicesToArchive(invoiceId: Long) // moved to rabbit mq

    @NewSpan
    @Topic("post-restore-utr")
    fun emitPostRestoreUtr(restoreUtrResponse: RestoreUtrResponse) // already in rabbit

    @NewSpan
    @Topic("settlement-migration")
    fun emitSettlementRecord(settlementRecord: SettlementRecord) // moved to rabbitMQ

    @NewSpan
    @Topic("update-utilization-amount")
    fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest) // moved to rabbitMQ

    @NewSpan
    @Topic("update-invoice-status-migration")
    fun emitInvoiceStatus(paidUnpaidStatus: PaidUnpaidStatus) // moved to rabbitMQ

    @NewSpan
    @Topic("update-bill-status-migration")
    fun emitBIllStatus(paidUnpaidStatus: PaidUnpaidStatus) // moved to rabbitMQ

    @NewSpan
    @Topic("unfreeze-credit-consumpation")
    fun emitUnfreezeCreditConsumption(request: Settlement) // moved to rabbitMQ

    @NewSpan
    @Topic("update-supplier-details")
    fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) // moved to rabbitMQ
}
