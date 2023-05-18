package com.cogoport.ares.model.payment.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class AccPayablesOfOrgRes(
    val entityCode: Int,
    val ledCurrency: String,
    var accountPayables: BigDecimal
)
