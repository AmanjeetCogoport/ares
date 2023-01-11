package com.cogoport.ares.api.common.config
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelInitializer
import jakarta.inject.Singleton

@Singleton
class RabbitMqBootstrap : ChannelInitializer() {

    override fun initialize(channel: Channel?, name: String) {
        channel?.exchangeDeclare("ares", "topic", true)
        channel?.queueDeclare("update-supplier-details", true, false, false, null)
        channel?.queueBind("update-supplier-details", "ares", "supplier-outstanding", null)
    }
}
