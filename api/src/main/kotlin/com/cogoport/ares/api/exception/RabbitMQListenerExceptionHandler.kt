package com.cogoport.ares.api.exception

import com.cogoport.brahma.rabbitmq.exception.RabbitMqCustomListenerErrorHandler
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.rabbitmq.exception.DefaultRabbitListenerExceptionHandler
import jakarta.inject.Singleton

@Factory
class RabbitMQListenerExceptionHandler {

    @Replaces(DefaultRabbitListenerExceptionHandler::class)
    @Singleton
    fun handlerConfig(): RabbitMqCustomListenerErrorHandler {
        return RabbitMqCustomListenerErrorHandler()
    }
}
