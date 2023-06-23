package com.cogoport.ares.api.events
import com.cogoport.ares.api.migration.model.PaidUnpaidStatus
import com.cogoport.ares.model.payment.RestoreUtrResponse
import com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent
import com.cogoport.ares.model.payment.response.OnAccountWithUtrResponse
import com.cogoport.kuber.model.bills.request.UpdatePaymentStatusRequest
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("kuber")
@RabbitProperty(name = "deliveryMode", value = "2")
@MessageHeader(name = "x-retry-count", value = "0")
interface KuberMessagePublisher {
    @Binding("kuber.post.restore.utr")
    suspend fun emitPostRestoreUtr(restoreUtrResponse: RestoreUtrResponse)

    @Binding("kuber.update.bill.archive")
    suspend fun emitUpdateBillsToArchive(billId: Long)

    @Binding("kuber.payables.bill.status")
    suspend fun emitBillPaymentStatus(payableProduceEvent: PayableKnockOffProduceEvent)

    @Binding("kuber.update.bill.payment.status")
    suspend fun emitUpdateBillPaymentStatus(updatePaymentStatusRequest: UpdatePaymentStatusRequest)

    @Binding("kuber.update.bill.status.migration")
    suspend fun emitBIllStatus(paidUnpaidStatus: PaidUnpaidStatus)

    @Binding("kuber.update.advance.document.status")
    suspend fun updateAdvanceDocumentStatus(request: OnAccountWithUtrResponse)
}
