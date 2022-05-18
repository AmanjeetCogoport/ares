package com.cogoport.ares.payment.model

import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class SalesTrend (
        val month: String,
        val salesOnCredit: Float
        )