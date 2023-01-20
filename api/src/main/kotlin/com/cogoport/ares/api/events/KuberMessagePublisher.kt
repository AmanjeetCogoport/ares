package com.cogoport.ares.api.events
import com.cogoport.ares.model.payment.RestoreUtrResponse
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("kuber")
@RabbitProperty(name = "deliveryMode", value = "2")
interface KuberMessagePublisher {
    @Binding("restore.utr")
    fun emitPostRestoreUtr(restoreUtrResponse: RestoreUtrResponse)

    @Binding("update.bill.archive")
    fun emitUpdateBillsToArchive(billId: Long)
}
