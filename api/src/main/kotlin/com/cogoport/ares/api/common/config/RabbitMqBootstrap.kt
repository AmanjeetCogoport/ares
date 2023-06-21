package com.cogoport.ares.api.common.config
import com.cogoport.ares.api.common.AresConstants
import com.cogoport.brahma.rabbitmq.client.implementation.RabbitmqServiceImpl
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class RabbitMqBootstrap {
    @Singleton
    fun handleBootstrap(): RabbitmqServiceImpl {
        return RabbitmqServiceImpl(AresConstants.ARES_EXCHANGE, AresConstants.QUEUES, AresConstants.RETRY_QUEUES)
    }
}
