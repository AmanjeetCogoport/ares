package com.cogoport.ares.model.dunning.response

import io.micronaut.core.annotation.Introspected

@Introspected
data class StatsResponse(
    val emailsDelivered: Long?,
    val emailsBounced: Long?,
    val emailsOpened: Long?,
    val emailsSent: Long?,
    val paymentInitiated: Long?,
    val paymentDone: Long?,
)
