package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
@MappedEntity
data class DailyOutstanding(
    var month: Int,
    var openInvoiceAmount: BigDecimal?,
    var onAccountPayment: BigDecimal?,
    var outstandings: BigDecimal?,
    var totalSales: BigDecimal?,
    var days: Int,
    var value: Double,
    val serviceType: ServiceType?,
    val currency: String?
)
