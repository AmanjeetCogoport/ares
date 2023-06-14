package com.cogoport.ares.api.events

import com.cogoport.ares.api.common.client.AuthClient
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
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

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

    @Queue("update-supplier-details", prefetch = 1)
    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateSupplierDetails(request.orgId.toString(), false, null)
    }

    @Queue("knockoff-payables", prefetch = 1)
    fun knockoffPayables(knockOffUtilizationEvent: KnockOffUtilizationEvent) = runBlocking {
        knockoffService.uploadBillPayment(knockOffUtilizationEvent.knockOffUtilizationRequest)
    }

    @Queue("reverse-utr", prefetch = 1)
    fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) = runBlocking {
        knockoffService.reverseUtr(reverseUtrRequest)
    }

    @Queue("unfreeze-credit-consumption", prefetch = 1)
    fun unfreezeCreditConsumption(request: Settlement) = runBlocking {
        settlementService.sendKnockOffDataToCreditConsumption(request)
    }

    @Queue("receivables-outstanding-data", prefetch = 1)
    fun listenOutstandingData(openSearchEvent: OpenSearchEvent) = runBlocking {
        openSearchService.pushOutstandingData(openSearchEvent.openSearchRequest)
    }

    @Queue("update-utilization-amount", prefetch = 1)
    fun updateUtilizationAmount(payLocUpdateRequest: PayLocUpdateRequest) = runBlocking {
        paymentMigration.updatePayment(payLocUpdateRequest)
    }

    /*For Saving  both Account Payables and Account Receivables bills/invoices amount */
    @Queue("create-account-utilization", prefetch = 1)
    fun listenCreateAccountUtilization(accountUtilizationEvent: AccountUtilizationEvent) = runBlocking {
        accountUtilService.add(accountUtilizationEvent.accUtilizationRequest)
    }

    /*For updating  both Account Payables and Account Receivables bills/invoices amount */
    @Queue("update-account-utilization", prefetch = 1)
    fun listenUpdateAccountUtilization(updateInvoiceEvent: UpdateInvoiceEvent) = runBlocking {
        accountUtilService.update(updateInvoiceEvent.updateInvoiceRequest)
    }

    @Queue("delete-account-utilization", prefetch = 1)
    fun listenDeleteAccountUtilization(deleteInvoiceEvent: DeleteInvoiceEvent) = runBlocking {
        accountUtilService.delete(deleteInvoiceEvent.deleteInvoiceRequest)
    }

    @Queue("update-account-status", prefetch = 1)
    fun listenUpdateInvoiceStatus(updateInvoiceStatusEvent: UpdateInvoiceStatusEvent) = runBlocking {
        accountUtilService.updateStatus(updateInvoiceStatusEvent.updateInvoiceStatusRequest)
    }

    @Queue("settlement-migration", prefetch = 1)
    fun migrateSettlements(settlementRecord: SettlementRecord) = runBlocking {
        paymentMigration.migrateSettlements(settlementRecord)
    }

    @Queue("sage-payment-migration", prefetch = 1)
    fun migrateSagePayments(paymentRecord: PaymentRecord) = runBlocking {
        paymentMigration.migratePayment(paymentRecord)
    }

    @Queue("sage-jv-migration", prefetch = 1)
    fun migrateJournalVoucher(journalVoucherRecord: JVParentDetails) = runBlocking {
        paymentMigration.migrateJV(journalVoucherRecord)
    }
    @Queue("send-payment-details-for-autoKnockOff", prefetch = 1)
    fun settleWithSourceIdAndDestinationId(autoKnockOffRequest: AutoKnockOffRequest) = runBlocking {
        settlementService.settleWithSourceIdAndDestinationId(autoKnockOffRequest)
    }
    @Queue("update-customer-details", prefetch = 1)
    fun updateCustomerOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateCustomerDetails(request.orgId.toString(), false, null)
    }

    @Queue("delete-invoices-not-present-in-plutus", prefetch = 1)
    fun deleteInvoicesNotPresentInPlutus(id: Long) = runBlocking {
        accountUtilService.deleteInvoicesNotPresentInPlutus(id)
    }

    @Queue("migrate-settlement-number", prefetch = 1)
    fun migrateSettlementNum(id: Long) = runBlocking {
        paymentMigration.migrateSettlementNum(id)
    }

    @Queue("update-settlement-bill-updated", prefetch = 1)
    fun editSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent: UpdateSettlementWhenBillUpdatedEvent) = runBlocking {
        knockoffService.editSettlementWhenBillUpdated(updateSettlementWhenBillUpdatedEvent)
    }

    @Queue("tagged-bill-auto-knockoff", prefetch = 1)
    fun taggedBillAutoKnockOff(req: OnAccountPaymentRequest) = runBlocking {
        taggedSettlementService.settleOnAccountInvoicePayment(req)
    }

    @Queue("ares-migrate-gl-codes", prefetch = 1)
    fun migrateGLCode(req: GlCodeMaster) = runBlocking {
        paymentMigrationWrapper.createGLCode(req)
    }

    @Queue("ares-post-jv-to-sage", prefetch = 1)
    fun postJVToSage(req: PostJVToSageRequest) = runBlocking {
        parentJVService.postJVToSage(Hashids.decode(req.parentJvId)[0], req.performedBy)
    }

    @Queue("migrate-new-period", prefetch = 1)
    fun migrateNewPeriodRecord(newPeriodRecord: NewPeriodRecord) = runBlocking {
        paymentMigration.migrateNewPeriodRecords(newPeriodRecord)
    }

    @Queue("migrate-jv-pay-loc", prefetch = 1)
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

    @Queue("ares-bulk-post-settlement-to-sage", prefetch = 1)
    fun bulkMatchingSettlementOnSage(req: PostSettlementRequest) = runBlocking {
        settlementService.matchingSettlementOnSage(req.settlementId, req.performedBy)
    }

    @Queue("ares-partial-payment-mismatch", prefetch = 1)
    fun partialPaymentMismatchDocument(documentNo: String) = runBlocking {
        paymentMigration.partialPaymentMismatchDocument(documentNo.toLong())
    }
    @Queue("ares-send-email", prefetch = 1)
    fun sendEmail(req: CreateCommunicationRequest) = runBlocking {
        authClient.sendCommunication(req)
    }
}
