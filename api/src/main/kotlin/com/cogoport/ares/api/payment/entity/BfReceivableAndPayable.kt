package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class BfReceivableAndPayable(
    var nonOverdueAmount: BigDecimal,
    var overdueAmount: BigDecimal,
    var notPaidDocumentCount: Long,
    var thirtyDayOverdue: BigDecimal,
    var sixtyDayOverdue: BigDecimal,
    var ninetyDayOverdue: BigDecimal,
    var oneEightyDayOverdue: BigDecimal,
    var threeSixtyDayOverdue: BigDecimal,
    var threeSixtyPlusDayOverdue: BigDecimal,
    var totalOceanImportDue: BigDecimal?,
    var totalOceanExportDue: BigDecimal?,
    var totalAirImportDue: BigDecimal?,
    var totalAirExportDue: BigDecimal?,
    var totalAirOthersDue: BigDecimal?,
    var totalSurfaceDomesticDue: BigDecimal?,
    var totalSurfaceLocalDue: BigDecimal?
)
