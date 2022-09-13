package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverdueInvoices(
    val ThirtyAmount: BigDecimal?,
    val SixtyAmount: BigDecimal?,
    val NinetyAmount: BigDecimal?,
    val NinetyPlusAmount: BigDecimal?,
    val ThirtyCount: Int?,
    val SixtyCount: Int?,
    val NinetyCount: Int?,
    val NinetyPlusCount: Int?
)
