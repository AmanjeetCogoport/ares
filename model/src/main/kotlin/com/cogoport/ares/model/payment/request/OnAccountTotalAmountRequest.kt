package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class OnAccountTotalAmountRequest(
    val accType: String,
    val orgIdList: List<String>,
    val accMode: String
)
