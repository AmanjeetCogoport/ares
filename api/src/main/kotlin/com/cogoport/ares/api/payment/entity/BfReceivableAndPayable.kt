package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class BfReceivableAndPayable(
    var nonOverdueAmount: BigDecimal? = 0.toBigDecimal(),
    var overdueAmount: BigDecimal? = 0.toBigDecimal(),
    var notPaidDocumentCount: Long? = 0,
    var thirtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var sixtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var ninetyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var oneEightyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var threeSixtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var threeSixtyPlusDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var totalOceanImportDue: BigDecimal? = 0.toBigDecimal(),
    var totalOceanExportDue: BigDecimal? = 0.toBigDecimal(),
    var totalAirImportDue: BigDecimal? = 0.toBigDecimal(),
    var totalAirExportDue: BigDecimal? = 0.toBigDecimal(),
    var totalAirOthersDue: BigDecimal? = 0.toBigDecimal(),
    var totalSurfaceDomesticDue: BigDecimal? = 0.toBigDecimal(),
    var totalSurfaceLocalDue: BigDecimal? = 0.toBigDecimal()
)
