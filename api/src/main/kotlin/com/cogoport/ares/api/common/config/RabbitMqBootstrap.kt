package com.cogoport.ares.api.common.config
import com.cogoport.ares.api.utils.logger
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelInitializer
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton

@Singleton
class RabbitMqBootstrap : ChannelInitializer() {

    @EventListener
    fun onStartupEvent(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
        logger().info("Log from rabbit mq bootstrapper")
        initialize(null, "")
    }

    override fun initialize(channel: Channel?, name: String) {
        channel?.exchangeDeclare("ares", "topic", true)
        channel?.queueDeclare("update-supplier-details", true, false, false, null)
        channel?.queueBind("update-supplier-details", "ares", "supplier.outstanding", null)
    }
}
