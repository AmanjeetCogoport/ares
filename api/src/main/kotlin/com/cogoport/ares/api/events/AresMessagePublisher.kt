package com.cogoport.ares.api.events

import com.cogoport.ares.api.common.models.FindRecordByDocumentNo
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq
import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.GlCodeMaster
import com.cogoport.ares.model.settlement.PostJVToSageRequest
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.PostSettlementRequest
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("ares")
@RabbitProperty(name = "deliveryMode", value = "2")
@MessageHeader(name = "x-retry-count", value = "0")
interface AresMessagePublisher {
    @Binding("ares.update.supplier.details")
    suspend fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest)

    @Binding("ares.unfreeze.credit.consumption")
    suspend fun emitUnfreezeCreditConsumption(request: Settlement)

    @Binding("ares.unfreeze.debit.consumption")
    suspend fun emitUnfreezeDebitConsumption(request: FindRecordByDocumentNo)

    @Binding("ares.receivables.outstanding.data")
    suspend fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Binding("ares.update.utilization.amount")
    suspend fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest)

    @Binding("ares.settlement.migration")
    suspend fun emitSettlementRecord(settlementRecord: SettlementRecord)

    @Binding("ares.sage.payment.migration")
    suspend fun emitPaymentMigration(paymentRecord: PaymentRecord)

    @Binding("ares.sage.jv.migration")
    suspend fun emitJournalVoucherMigration(journalVoucherRecord: JVParentDetails)
    suspend fun emitJournalVoucherMigration(journalVoucherRecord: JournalVoucherRecord)

    @Binding("ares.update.customer.details")
    suspend fun emitUpdateCustomerOutstanding(request: UpdateSupplierOutstandingRequest)

    @Binding("ares.delete.invoices.not.present.in.plutus")
    suspend fun emitDeleteInvoicesNotPresentInPlutus(id: Long)

    @Binding("ares.migrate.settlement.number")
    suspend fun emitMigrateSettlementNumber(ids: Long)

    @Binding("ares.update.settlement.bill.updated")
    suspend fun emitUpdateSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent: UpdateSettlementWhenBillUpdatedEvent)
    @Binding("ares.tagged.bill.auto.knockoff")
    suspend fun emitTaggedBillAutoKnockOff(req: OnAccountPaymentRequest)
    @Binding("ares.migrate.new.period")
    suspend fun emitNewPeriodRecords(newPeriodRecord: NewPeriodRecord)
    @Binding("ares.migrate.jv.pay.loc")
    suspend fun emitJVUtilization(record: JVRecordsScheduler)

    @Binding("ares.migrate.gl.codes")
    suspend fun emitGLCode(req: GlCodeMaster)

    @Binding("ares.upsert.migrate.glcodes")
    suspend fun emitUpsertMigrateGlCode(req: GlCodeMaster)

    @Binding("ares.post.jv.to.sage")
    suspend fun emitPostJvToSage(req: PostJVToSageRequest)

    @Binding("ares.post.payment.to.sage")
    suspend fun emitPostPaymentToSage(req: PostPaymentToSage)

    @Binding("ares.sage.payment.num.migration")
    suspend fun emitSagePaymentNumMigration(paymentRecord: SagePaymentNumMigrationResponse)

    @Binding("ares.bulk.update.payment.and.post.on.sage")
    suspend fun emitBulkUpdatePaymentAndPostOnSage(req: PostPaymentToSage)

    @Binding("ares.bulk.post.payment.to.sage")
    suspend fun emitBulkPostPaymentToSage(req: PostPaymentToSage)

    @Binding("ares.bulk.post.payment.from.sage")
    suspend fun emitBulkPostPaymentFromSage(req: PostPaymentToSage)

    @Binding("ares.bulk.post.settlement.to.sage")
    suspend fun emitBulkMatchingSettlementOnSage(req: PostSettlementRequest)

    @Binding("ares.partial.payment.mismatch")
    suspend fun emitPartialPaymentMismatchDocument(id: String)

    @Binding("ares.send.email")
    suspend fun sendEmail(req: CreateCommunicationRequest)

    @Binding("ares.sage.tds.jv.migration")
    suspend fun emitTDSJournalVoucherMigration(journalVoucherRecord: JVParentDetails)

    @Binding("ares.dunning.scheduler")
    suspend fun scheduleDunning(req: CycleExecutionProcessReq)

    @Binding("ares.send.dunning.payment.reminder")
    suspend fun sendPaymentReminder(request: PaymentReminderReq)

    @Binding("ares.send.mail.of.all.communication.of.tradeparty")
    suspend fun sendMailOfAllCommunicationOfTradeParty(
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq
    )

    @Binding("ares.sage.jv.migration.admin")
    suspend fun emitJournalVoucherMigrationAdmin(journalVoucherRecord: JVParentDetails)

    @Binding("ares.migrate.payment.amount")
    suspend fun emitMigratePaymentAmount(id: Long)

    @Binding("ares.send.payment.details.for.autoKnockOff")
    suspend fun emitSendPaymentDetailsForKnockOff(autoKnockOffRequest: AutoKnockOffRequest)
}
