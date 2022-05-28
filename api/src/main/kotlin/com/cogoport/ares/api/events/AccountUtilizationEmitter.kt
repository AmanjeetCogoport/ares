package com.cogoport.ares.api.events

import com.cogoport.ares.model.payment.AccountUtilizationEvent
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic

// Need to move this to plustus
@KafkaClient
interface AccountUtilizationEmitter {

    @Topic("account-utilization")
    fun emitAccountUtilizationEvent(accountUtilizationEvent: AccountUtilizationEvent)
}
