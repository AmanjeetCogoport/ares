package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverallStatsForCustomerResponse(
    var custId: String?,
    var kamProformaCount: CustomerListCountResponse?,
    var balance: StatsForCustomerResponse?,
    var custName: String? = null
)
