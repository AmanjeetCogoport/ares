package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class SupplierOutstandingAgeingBucket(
        val taggedOrganizationId: String?,
        val notDueAmount: BigDecimal,
        val thirtyAmount: BigDecimal,
        val fortyfiveAmount: BigDecimal,
        val sixtyAmount: BigDecimal,
        val ninetyAmount: BigDecimal,
        val oneeightyAmount: BigDecimal,
        val threesixtyfiveAmount: BigDecimal,
        val threesixtyfiveplusAmount: BigDecimal,
        val notDueCount: Int,
        val thirtyCount: Int,
        val fortyfiveCount: Int,
        val sixtyCount: Int,
        val ninetyCount: Int,
        val oneeightyCount: Int,
        val threesixtyfiveCount: Int,
        val threesixtyfiveplusCount: Int
)
