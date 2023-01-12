package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Inject

@RabbitListener
class AresMessageConsumer {
    @Inject
    lateinit var outstandingService: OutStandingService
    @Queue("update-supplier-details")
    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) {
        logger().info("here")
//        outstandingService.updateSupplierDetails(request.orgId.toString(), false, null)
    }
}
