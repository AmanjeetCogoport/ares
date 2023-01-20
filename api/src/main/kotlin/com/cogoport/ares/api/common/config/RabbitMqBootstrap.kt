package com.cogoport.ares.api.common.config
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelInitializer
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton

@Singleton
class RabbitMqBootstrap : ChannelInitializer() {

    @EventListener
    fun onStartupEvent(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
        initialize(null, "")
    }

    override fun initialize(channel: Channel?, name: String) {
        channel?.exchangeDeclare("ares", "topic", true)

        channel?.queueDeclare("update-supplier-details", true, false, false, null)
        channel?.queueBind("update-supplier-details", "ares", "supplier.outstanding", null)

        channel?.queueDeclare("knockoff-payables", true, false, false, null)
        channel?.queueBind("knockoff-payables", "ares", "knockoff.payables", null)

        channel?.queueDeclare("reverse-utr", true, false, false, null)
        channel?.queueBind("reverse-utr", "ares", "reverse.utr", null)

        channel?.queueDeclare("unfreeze-credit-consumption", true, false, false, null)
        channel?.queueBind("unfreeze-credit-consumption", "ares", "unfreeze.credit.consumption", null)

        channel?.queueDeclare("receivables-dashboard-data", true, false, false, null)
        channel?.queueBind("receivables-dashboard-data", "ares", "receivables.dashboard.data", null)

        channel?.queueDeclare("receivables-outstanding-data", true, false, false, null)
        channel?.queueBind("receivables-outstanding-data", "ares", "receivables.outstanding.data", null)
    }
}
