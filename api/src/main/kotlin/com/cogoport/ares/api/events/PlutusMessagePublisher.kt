package com.cogoport.ares.api.events
import com.cogoport.ares.api.migration.model.PaidUnpaidStatus
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.plutus.model.invoice.InvoiceStatusUpdateRequest
import com.cogoport.plutus.model.invoice.request.UpdateInvoiceByProformaRequest
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("plutus")
@RabbitProperty(name = "deliveryMode", value = "2")
@MessageHeader(name = "x-retry-count", value = "0")
interface PlutusMessagePublisher {
    @Binding("plutus.update.invoice.archive")
    suspend fun emitUpdateInvoicesToArchive(invoiceId: Long)

    @Binding("plutus.update.invoice.balance")
    suspend fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent)

    @Binding("plutus.update.invoice.status.migration")
    suspend fun emitInvoiceStatus(paidUnpaidStatus: PaidUnpaidStatus)

    @Binding("plutus.update.status")
    suspend fun emitUpdateStatus(invoiceStatusUpdateRequest : InvoiceStatusUpdateRequest)
}
