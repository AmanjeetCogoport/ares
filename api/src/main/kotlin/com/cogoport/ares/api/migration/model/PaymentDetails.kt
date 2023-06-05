package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class PaymentDetails(
    val id: Long,
    val paymentNum: Long,
    val documentNo: Long?,
    val transRefNumber: String,
    val amount: BigDecimal = BigDecimal.ZERO,
    val unutilisedAmount: BigDecimal = BigDecimal.ZERO
)
