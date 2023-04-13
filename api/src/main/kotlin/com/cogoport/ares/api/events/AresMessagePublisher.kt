package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.GlCodeMaster
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
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

    @Binding("receivables.outstanding.data")
    suspend fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Binding("update.utilization.amount")
    suspend fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest)

    @Binding("settlement.migration")
    suspend fun emitSettlementRecord(settlementRecord: SettlementRecord)

    @Binding("sage.payment.migration")
    suspend fun emitPaymentMigration(paymentRecord: PaymentRecord)

    @Binding("sage.jv.migration")
    suspend fun emitJournalVoucherMigration(journalVoucherRecord: JVParentDetails)
    suspend fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord)

    @Binding("customer.outstanding")
    suspend fun emitUpdateCustomerOutstanding(request: UpdateSupplierOutstandingRequest)

    @Binding("delete.invoices.not.present.in.plutus")
    suspend fun emitDeleteInvoicesNotPresentInPlutus(id: Long)

    @Binding("migrate.settlement.number")
    suspend fun emitMigrateSettlementNumber(ids: Long)

    @Binding("update.settlement.bill.updated")
    suspend fun emitUpdateSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent: UpdateSettlementWhenBillUpdatedEvent)
    @Binding("tagged.bill.auto.knockoff")
    suspend fun emitTaggedBillAutoKnockOff(req: OnAccountPaymentRequest)

    @Binding("migrate.gl.codes")
    suspend fun emitGLCode(req: GlCodeMaster)
}
