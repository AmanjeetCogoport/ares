package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.configuration.kafka.annotation.Topic
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty

@RabbitClient("ares")
@RabbitProperty(name = "deliveryMode", value = "2")
interface AresMessagePublisher {
    @Binding("supplier.outstanding")
    fun emitUpdateSupplierOutstanding(request: UpdateSupplierOutstandingRequest)

    @Binding("unfreeze.credit.consumption")
    fun emitUnfreezeCreditConsumption(request: Settlement)

    @Binding("receivables.dashboard.data")
    fun emitDashboardData(openSearchEvent: OpenSearchEvent)

    @Binding("receivables.outstanding.data")
    fun emitOutstandingData(openSearchEvent: OpenSearchEvent)

    @Binding("update-utilization-amount")
    fun emitUtilizationUpdateRecord(payLocUpdateRequest: PayLocUpdateRequest)
}
