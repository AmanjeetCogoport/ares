package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("ares")
@RabbitProperty(name = "deliveryMode", value = "2")
@MessageHeader(name = "x-retry-count", value = "0")
interface AresMessagePublisher {
    @Binding("supplier.outstanding")
    suspend fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest)

    @Binding("unfreeze.credit.consumption")
    suspend fun emitUnfreezeCreditConsumption(request: Settlement)

    @Binding("receivables.dashboard.data")
    suspend fun emitDashboardData(openSearchEvent: OpenSearchEvent)

    @Binding("receivables.outstanding.data")
    suspend fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Binding("update.utilization.amount")
    suspend fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest)

    @Binding("settlement.migration")
    suspend fun emitSettlementRecord(settlementRecord: SettlementRecord)

    @Binding("sage.payment.migration")
    suspend fun emitPaymentMigration(paymentRecord: PaymentRecord)

    @Binding("sage.jv.migration")
    suspend fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord)

    @Binding("send.payment.details.for.autoKnockOff")
    suspend fun emitSettleForAutoKnockOff(autoKnockOffRequest: AutoKnockOffRequest)

    @Binding("update.settlement.bill.updated")
    suspend fun emitUpdateSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent: UpdateSettlementWhenBillUpdatedEvent)
}
