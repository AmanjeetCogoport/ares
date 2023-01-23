package com.cogoport.ares.api.events
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("Plutus")
@RabbitProperty(name = "deliveryMode", value = "2")
interface PlutusMessagePublisher {
    @Binding("update.invoice.archive")
    fun emitUpdateInvoicesToArchive(invoiceId: Long)

    @Binding("update-invoice-balance")
    fun emitInvoiceBalance(invoiceBalanceEvent: UpdateInvoiceBalanceEvent)
}
