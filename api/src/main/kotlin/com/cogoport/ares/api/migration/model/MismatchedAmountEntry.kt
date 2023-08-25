package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class MismatchedAmountEntry(
    val id: Long?,
    val amount: BigDecimal = BigDecimal.ZERO,
    val ledAmount: BigDecimal = BigDecimal.ZERO,
    val documentNo: Long?,
    val documentValue: String?,
)
