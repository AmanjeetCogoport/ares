package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OverallStats(
    val totalOutstandingAmount: BigDecimal?,
    val openInvoicesCount: Int?,
    val organizationCount: Int?,
    val openInvoicesAmount: BigDecimal?,
    val openOnAccountPaymentAmount: BigDecimal?,
    var id: String?
)
