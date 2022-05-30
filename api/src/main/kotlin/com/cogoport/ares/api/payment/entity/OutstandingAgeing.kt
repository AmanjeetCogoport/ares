package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
@MappedEntity
data class OutstandingAgeing(
    val organization_id: String?,
    val not_due_amount: BigDecimal?,
    val thirty_amount: BigDecimal?,
    val sixty_amount: BigDecimal?,
    val ninety_amount: BigDecimal?,
    val oneeighty_amount: BigDecimal?,
    val threesixfive_amount: BigDecimal?,
    val threesixfiveplus_amount: BigDecimal?,
    val not_due_count: Int,
    val thirty_count: Int,
    val sixty_count: Int,
    val ninety_count: Int,
    val oneeighty_count: Int,
    val threesixfive_count: Int,
    val threesixfiveplus_count: Int

)
