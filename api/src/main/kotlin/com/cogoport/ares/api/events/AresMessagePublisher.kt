package com.cogoport.ares.api.events

import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("ares")
@RabbitProperty(name = "deliveryMode", value = "2")
interface AresMessagePublisher {
    @Binding("supplier-outstanding")
    fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest)
}
