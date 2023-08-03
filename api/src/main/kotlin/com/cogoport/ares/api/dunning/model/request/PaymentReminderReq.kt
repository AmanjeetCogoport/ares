package com.cogoport.ares.api.dunning.model.request

import java.util.UUID

data class PaymentReminderReq(
    val cycleExecutionId: Long,
    val tradePartyDetailId: UUID
)
