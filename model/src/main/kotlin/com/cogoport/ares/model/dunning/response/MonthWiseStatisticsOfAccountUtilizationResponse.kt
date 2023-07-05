package com.cogoport.ares.model.dunning.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class MonthWiseStatisticsOfAccountUtilizationResponse(
    var month: String,
    var collectedAmount: BigDecimal?,
    var openInvoiceAmount: BigDecimal?,
    var outstandingAmount: BigDecimal?,
    var year: String
)
