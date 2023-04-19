package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.model.payment.AccountUtilizationEvent
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.event.DeleteInvoiceEvent
import com.cogoport.ares.model.payment.event.KnockOffUtilizationEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceEvent
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusEvent
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
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
    lateinit var accountUtilService: AccountUtilizationService

    @Inject
    lateinit var taggedSettlementService: TaggedSettlementService

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
}
