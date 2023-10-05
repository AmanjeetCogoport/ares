package com.cogoport.ares.api.events

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq
import com.cogoport.ares.api.dunning.service.interfaces.DunningHelperService
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.payment.event.DeleteInvoiceEvent
import com.cogoport.ares.model.payment.event.KnockOffUtilizationEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusEvent
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.GlCodeMaster
import com.cogoport.ares.model.settlement.PostJVToSageRequest
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.PostSettlementRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.rabbitmq.model.RabbitmqEventLogDocument
import com.cogoport.plutus.model.invoice.request.IrnGenerationEmailRequest
import com.rabbitmq.client.Envelope
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import java.util.Date

@RabbitListener
class AresMessageConsumer {

    @Inject
    lateinit var outstandingService: OutStandingService

    @Inject
    lateinit var knockoffService: KnockoffService

    @Inject
    private lateinit var settlementService: SettlementService

    @Inject
    private lateinit var openSearchService: OpenSearchService

    @Inject
    lateinit var paymentMigration: PaymentMigration

    @Inject
    lateinit var onAccountService: OnAccountService

    @Inject
    lateinit var accountUtilService: AccountUtilizationService

    @Inject
    lateinit var taggedSettlementService: TaggedSettlementService

    @Inject
    lateinit var paymentMigrationWrapper: PaymentMigrationWrapper

    @Inject
    lateinit var parentJVService: ParentJVService

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var scheduleService: ScheduleService

    @Inject
    lateinit var dunningHelperService: DunningHelperService

    @Queue("ares-update-supplier-details", prefetch = 1)
    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateSupplierDetails(request.orgId.toString(), false, null)
    }

    @Queue("ares-knockoff-payables", prefetch = 1)
    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
    }

    @Queue("ares-reverse-utr", prefetch = 1)
    fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) = runBlocking {
        knockoffService.reverseUtr(reverseUtrRequest)
    }

    @Queue("ares-unfreeze-credit-consumption", prefetch = 1)
    fun unfreezeCreditConsumption(request: Settlement) = runBlocking {
        settlementService.sendKnockOffDataToCreditConsumption(request)
    }

    @Queue("ares-receivables-outstanding-data", prefetch = 1)
    fun listenOutstandingData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushOutstandingData(openSearchEvent.openSearchRequest)
    }

    @Queue("ares-update-utilization-amount", prefetch = 1)
    fun updateUtilizationAmount(payLocUpdateRequest: PayLocUpdateRequest) = runBlocking {
        paymentMigration.updatePayment(payLocUpdateRequest)
    }

    /*For Saving  both Account Payables and Account Receivables bills/invoices amount */
    @Queue("ares-create-account-utilization", prefetch = 1)
    fun listenCreateAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        accountUtilService.add(accountUtilizationEvent.accUtilizationRequest)
    }

    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
    @Queue("ares-update-account-utilization", prefetch = 1)
    fun listenUpdateAccountUtilization(updateInvoiceEvent: UpdateInvoiceEvent) = runBlocking {
        accountUtilService.update(updateInvoiceEvent.updateInvoiceRequest)
    }

    @Queue("ares-delete-account-utilization", prefetch = 1)
    fun listenDeleteAccountUtilization(deleteInvoiceEvent: DeleteInvoiceEvent) = runBlocking {
        accountUtilService.delete(deleteInvoiceEvent.deleteInvoiceRequest)
    }

    @Queue("ares-update-account-status", prefetch = 1)
    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
        accountUtilService.updateStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
    }

    @Queue("ares-settlement-migration", prefetch = 1)
    fun migrateSettlements(settlementRecord: SettlementRecord) = runBlocking {
        paymentMigration.migrateSettlements(settlementRecord)
    }

    @Queue("ares-sage-payment-migration", prefetch = 1)
    fun migrateSagePayments(paymentRecord: PaymentRecord) = runBlocking {
        paymentMigration.migratePayment(paymentRecord)
    }

    @Queue("ares-sage-jv-migration", prefetch = 1)
    fun migrateJournalVoucher(journalVoucherRecord: JVParentDetails) = runBlocking {
        paymentMigration.migrateJV(journalVoucherRecord)
    }
    @Queue("ares-send-payment-details-for-autoKnockOff", prefetch = 1)
    fun settleWithSourceIdAndDestinationId(autoKnockOffRequest: AutoKnockOffRequest) = runBlocking {
        settlementService.settleWithSourceIdAndDestinationId(autoKnockOffRequest)
    }
    @Queue("ares-update-customer-details", prefetch = 1)
    fun updateCustomerOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateCustomerDetails(request.orgId.toString(), false, null)
    }

    @Queue("ares-delete-invoices-not-present-in-plutus", prefetch = 1)
    fun deleteInvoicesNotPresentInPlutus(id: Long) = runBlocking {
        accountUtilService.deleteInvoicesNotPresentInPlutus(id)
    }

    @Queue("ares-migrate-settlement-number", prefetch = 1)
    fun migrateSettlementNum(id: Long) = runBlocking {
        paymentMigration.migrateSettlementNum(id)
    }

    @Queue("ares-update-settlement-bill-updated", prefetch = 1)
    fun editSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent: UpdateSettlementWhenBillUpdatedEvent) = runBlocking {
        knockoffService.editSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent)
    }

    @Queue("ares-tagged-bill-auto-knockoff", prefetch = 1)
    fun taggedBillAutoKnockOff(req: OnAccountPaymentRequest) = runBlocking {
        taggedSettlementService.settleOnAccountInvoicePayment(req)
    }

    @Queue("ares-migrate-gl-codes", prefetch = 1)
    fun migrateGLCode(req: GlCodeMaster) = runBlocking {
        paymentMigrationWrapper.createGLCode(req)
    }

    @Queue("ares-upsert-migrate-glcodes", prefetch = 1)
    fun upsertMigrateGLCode(req: GlCodeMaster) = runBlocking {
        paymentMigrationWrapper.upsertMigrateGLCode(req)
    }

    @Queue("ares-post-jv-to-sage", prefetch = 1)
    fun postJVToSage(req: PostJVToSageRequest) = runBlocking {
        parentJVService.postJVToSage(Hashids.decode(req.parentJvId)[0], req.performedBy)
    }

    @Queue("ares-migrate-new-period", prefetch = 1)
    fun migrateNewPeriodRecord(newPeriodRecord: NewPeriodRecord) = runBlocking {
        paymentMigration.migrateNewPeriodRecords(newPeriodRecord)
    }

    @Queue("ares-migrate-jv-pay-loc", prefetch = 1)
    fun migrateJVPayLoc(record: JVRecordsScheduler) = runBlocking {
        paymentMigration.migrateJVUtilization(record)
    }

    @Queue("ares-send-payment-details", prefetch = 1)
    fun sendPaymentDetailsForOnAccount(req: Payment) = runBlocking {
        onAccountService.createPaymentEntryAndReturnUtr(req)
    }

    @Queue("ares-post-payment-to-sage", prefetch = 1)
    fun postPaymentToSage(req: PostPaymentToSage) = runBlocking {
        onAccountService.postPaymentToSage(req.paymentId, req.performedBy)
    }

    @Queue("ares-sage-payment-num-migration", prefetch = 1)
    fun sagePaymentNumMigration(paymentRecord: SagePaymentNumMigrationResponse) = runBlocking {
        paymentMigration.migrateSagePaymentNum(paymentRecord)
    }

    @Queue("ares-bulk-update-payment-and-post-on-sage", prefetch = 1)
    fun bulkApproveAndFinalPostToSage(req: PostPaymentToSage) = runBlocking {
        onAccountService.bulkUpdatePaymentAndPostOnSage(req)
    }

    @Queue("ares-bulk-post-payment-to-sage", prefetch = 1)
    fun bulkDirectFinalPostToSage(req: PostPaymentToSage) = runBlocking {
        onAccountService.directFinalPostToSage(req)
    }

    @Queue("ares-bulk-post-payment-from-sage", prefetch = 1)
    fun bulkPostPaymentFromSage(req: PostPaymentToSage) = runBlocking {
        onAccountService.postPaymentFromSage(arrayListOf(req.paymentId), req.performedBy)
    }

    @Queue("ares-bulk-post-settlement-to-sage", prefetch = 1, autoAcknowledgment = true)
    fun bulkMatchingSettlementOnSage(req: PostSettlementRequest) = runBlocking {
        settlementService.matchingSettlementOnSage(req.settlementId, req.performedBy)
    }

    @Queue("ares-partial-payment-mismatch", prefetch = 1)
    fun partialPaymentMismatchDocument(documentNo: String) = runBlocking {
        paymentMigration.partialPaymentMismatchDocument(documentNo.toLong())
    }

    @Queue("ares-event-log", prefetch = 1)
    fun consumeEvents(@MessageBody message: ByteArray, @RabbitHeaders headers: Map<String, Object>?, envelope: Envelope) = runBlocking {
        val rabbitmqEventLogDocument = RabbitmqEventLogDocument(
            messageBody = String(message, Charsets.UTF_8),
            envelope = envelope,
            headers = headers.toString(),
            createdAt = Date()
        )
        openSearchService.pushEventLogsToOpenSearch(rabbitmqEventLogDocument)
    }
    @Queue("ares-send-email", prefetch = 1)
    fun sendEmail(req: CreateCommunicationRequest) = runBlocking {
        authClient.sendCommunication(req)
    }

    @Queue("ares-sage-tds-jv-migration", prefetch = 1)
    fun migrateTDSJournalVoucher(journalVoucherRecord: JVParentDetails) = runBlocking {
        paymentMigration.migrateTDSJV(journalVoucherRecord)
    }

    @Queue("ares-dunning-scheduler", prefetch = 1)
    fun scheduleDunning(req: CycleExecutionProcessReq) = runBlocking {
        scheduleService.processCycleExecution(req)
    }

    @Queue("ares-send-dunning-payment-reminder", prefetch = 1)
    fun sendPaymentReminder(request: PaymentReminderReq) = runBlocking {
        scheduleService.sendPaymentReminderToTradeParty(request)
    }

    @Queue("ares-send-mail-of-all-communication-of-tradeparty", prefetch = 1)
    fun sendMailOfAllCommunicationToTradeParty(
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq
    ) = runBlocking {
        dunningHelperService.sendMailOfAllCommunicationToTradeParty(
            sendMailOfAllCommunicationToTradePartyReq,
            false
        )
    }

    @Queue("ares-sage-jv-migration-admin", prefetch = 1)
    fun migrateJournalVoucherAdmin(journalVoucherRecord: JVParentDetails) = runBlocking {
        paymentMigration.migrateAdminJV(journalVoucherRecord)
    }

    @Queue("ares-migrate-payment-amount", prefetch = 1)
    fun migratePaymentAmount(id: Long) = runBlocking {
        paymentMigration.mismatchAmount(id)
    }

    @Queue("ares-send-email-for-irn-generation", prefetch = 1)
    fun sendEmailForIrnGeneration(irnGenerationEmailRequest: IrnGenerationEmailRequest) = runBlocking {
        scheduleService.sendEmailForIrnGeneration(irnGenerationEmailRequest)
    }
}
