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

        channel?.exchangeDeclare("error-exchange", "topic", true)
        channel?.queueDeclare("ares-error-queue", true, false, false, null)
        channel?.queueBind("ares-error-queue", "error-exchange", "ares.error", null)

        channel?.queueDeclare("update-supplier-details", true, false, false, null)
        channel?.queueBind("update-supplier-details", "ares", "supplier.outstanding", null)

        channel?.queueDeclare("knockoff-payables", true, false, false, null)
        channel?.queueBind("knockoff-payables", "ares", "knockoff.payables", null)

        channel?.queueDeclare("reverse-utr", true, false, false, null)
        channel?.queueBind("reverse-utr", "ares", "reverse.utr", null)

        channel?.queueDeclare("unfreeze-credit-consumption", true, false, false, null)
        channel?.queueBind("unfreeze-credit-consumption", "ares", "unfreeze.credit.consumption", null)

        channel?.queueDeclare("receivables-outstanding-data", true, false, false, null)
        channel?.queueBind("receivables-outstanding-data", "ares", "receivables.outstanding.data", null)

        channel?.queueDeclare("update-utilization-amount", true, false, false, null)
        channel?.queueBind("update-utilization-amount", "ares", "update.utilization.amount", null)

        channel?.queueDeclare("create-account-utilization", true, false, false, null)
        channel?.queueBind("create-account-utilization", "ares", "create.account.utilization", null)

        channel?.queueDeclare("update-account-utilization", true, false, false, null)
        channel?.queueBind("update-account-utilization", "ares", "update.account.utilization", null)

        channel?.queueDeclare("delete-account-utilization", true, false, false, null)
        channel?.queueBind("delete-account-utilization", "ares", "delete.account.utilization", null)

        channel?.queueDeclare("update-account-status", true, false, false, null)
        channel?.queueBind("update-account-status", "ares", "update.account.status", null)

        channel?.queueDeclare("settlement-migration", true, false, false, null)
        channel?.queueBind("settlement-migration", "ares", "settlement.migration", null)

        channel?.queueDeclare("sage-payment-migration", true, false, false, null)
        channel?.queueBind("sage-payment-migration", "ares", "sage.payment.migration", null)

        channel?.queueDeclare("sage-jv-migration", true, false, false, null)
        channel?.queueBind("sage-jv-migration", "ares", "sage.jv.migration", null)

        channel?.queueDeclare("send-payment-details-for-autoKnockOff", true, false, false, null)
        channel?.queueBind("send-payment-details-for-autoKnockOff", "ares", "send.payment.details.for.autoKnockOff", null)

        channel?.queueDeclare("update-customer-details", true, false, false, null)
        channel?.queueBind("update-customer-details", "ares", "customer.outstanding", null)

        channel?.queueDeclare("migrate-settlement-number", true, false, false, null)
        channel?.queueBind("migrate-settlement-number", "ares", "migrate.settlement.number", null)

        channel?.queueDeclare("update-settlement-bill-updated", true, false, false, null)
        channel?.queueBind("update-settlement-bill-updated", "ares", "update.settlement.bill.updated", null)

        channel?.queueDeclare("tagged-bill-auto-knockoff", true, false, false, null)
        channel?.queueBind("tagged-bill-auto-knockoff", "ares", "tagged.bill.auto.knockoff", null)

        channel?.queueDeclare("delete-invoices-not-present-in-plutus", true, false, false, null)
        channel?.queueBind("delete-invoices-not-present-in-plutus", "ares", "delete.invoices.not.present.in.plutus", null)

        channel?.queueDeclare("ares-migrate-gl-codes", true, false, false, null)
        channel?.queueBind("ares-migrate-gl-codes", "ares", "ares.migrate.gl.codes", null)

        channel?.queueDeclare("ares-post-jv-to-sage", true, false, false, null)
        channel?.queueBind("ares-post-jv-to-sage", "ares", "ares.post.jv.to.sage", null)

        channel?.queueDeclare("migrate-new-period", true, false, false, null)
        channel?.queueBind("migrate-new-period", "ares", "migrate.new.period", null)

        channel?.queueDeclare("migrate-jv-pay-loc", true, false, false, null)
        channel?.queueBind("migrate-jv-pay-loc", "ares", "migrate.jv.pay.loc", null)

        channel?.queueDeclare("ares-send-payment-details", true, false, false, null)
        channel?.queueBind("ares-send-payment-details", "ares", "ares.send.payment.details", null)

        channel?.queueDeclare("ares-post-payment-to-sage", true, false, false, null)
        channel?.queueBind("ares-post-payment-to-sage", "ares", "ares.post.payment.to.sage", null)

        channel?.queueDeclare("ares-sage-payment-num-migration", true, false, false, null)
        channel?.queueBind("ares-sage-payment-num-migration", "ares", "ares.sage.payment.num.migration", null)

        channel?.queueDeclare("ares-bulk-post-payment-to-sage", true, false, false, null)
        channel?.queueBind("ares-bulk-post-payment-to-sage", "ares", "ares.bulk.post.payment.to.sage", null)

        channel?.queueDeclare("ares-bulk-post-settlement-to-sage", true, false, false, null)
        channel?.queueBind("ares-bulk-post-settlement-to-sage", "ares", "ares.bulk.post.settlement.to.sage", null)
    }
}
