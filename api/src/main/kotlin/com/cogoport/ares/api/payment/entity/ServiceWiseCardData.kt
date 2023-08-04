package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
@Introspected
data class ServiceWiseCardData(
    var totalOverdue: BigDecimal? = 0.toBigDecimal(),
    var totalImportDue: BigDecimal? = 0.toBigDecimal(),
    var totalExportDue: BigDecimal? = 0.toBigDecimal(),
    var totalOtherDue: BigDecimal? = 0.toBigDecimal(),
    var totalDomesticDue: BigDecimal? = 0.toBigDecimal(),
    var totalLocalDue: BigDecimal? = 0.toBigDecimal(),
    var currency: String? = "INR"
)
