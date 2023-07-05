package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class SendMailOfAllCommunicationToTradePartyReq(
    var tradePartyDetailId: UUID,
    var userEmail: String,
    var userId: UUID,
    var userName: String
)
