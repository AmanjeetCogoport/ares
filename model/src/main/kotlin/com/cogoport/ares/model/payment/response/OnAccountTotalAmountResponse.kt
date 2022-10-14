package com.cogoport.ares.model.payment.response

import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity(value = "account_utilizations")
data class OnAccountTotalAmountResponse(
    var organizationId: UUID,
    var accMode: String,
    var accType: String,
    var paymentValue: String
)

