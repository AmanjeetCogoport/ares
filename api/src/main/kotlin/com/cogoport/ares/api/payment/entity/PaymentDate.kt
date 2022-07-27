package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@MappedEntity
data class PaymentDate(
    var documentNo: Long?,
    var transactionDate: Timestamp?,
    var exchangeRate: BigDecimal?
)
