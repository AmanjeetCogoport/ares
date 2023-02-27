package com.cogoport.ares.api.exception

import com.cogoport.ares.api.utils.logger
import io.micronaut.context.annotation.Replaces
import io.micronaut.rabbitmq.exception.DefaultRabbitListenerExceptionHandler
import io.micronaut.rabbitmq.exception.RabbitListenerException
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler
import io.sentry.Sentry
import jakarta.inject.Singleton

@Replaces(DefaultRabbitListenerExceptionHandler::class)
@Singleton
class RabbitMQListenerExceptionHandler : RabbitListenerExceptionHandler {
    override fun handle(exception: RabbitListenerException) {
//        val envelope = exception.messageState?.get()?.envelope
//        // Give logic for error exchanges here right now returning from here only
//        if (envelope?.exchange == "error-exchange") {
//            handleNormal(exception)
//        }
//        val data = exception.messageState?.get()?.body
//        val channel = exception.messageState?.get()?.channel
//        val properties = exception.messageState?.get()?.properties
//
//        val retryCount = properties?.headers?.get("x-retry-count").toString().toIntOrNull() ?: 0
//        if (retryCount < 3) {
//            logger().info("Here in retry handler -> $envelope -> ${exception.listener} -> ${exception.stackTraceToString()}")
//            properties?.headers?.set("x-retry-count", retryCount + 1)
//            channel?.basicPublish(envelope?.exchange, envelope?.routingKey, properties, data)
//        } else {
//            val messageEnvelope = hashMapOf(
//                "deliveryTag" to envelope?.deliveryTag,
//                "exchange" to envelope?.exchange,
//                "routingKey" to envelope?.routingKey,
//                "redeliver" to envelope?.isRedeliver
//            )
//            properties?.headers?.set("x-info", messageEnvelope)
//            channel?.basicPublish("error-exchange", "${envelope?.exchange}.error", properties, data)
//            handleNormal(exception)
//        }
    }
    private fun handleNormal(exception: RabbitListenerException) {
//        val messageState = exception.messageState
//        if (messageState.isPresent) {
//            logger().error(
//                "Error processing a message for RabbitMQ consumer [" + exception.listener + "]",
//                exception
//            )
//        } else {
//            logger().error(
//                "RabbitMQ consumer [" + exception.listener + "] produced an error",
//                exception
//            )
//        }
//        Sentry.captureException(exception)
    }
}
