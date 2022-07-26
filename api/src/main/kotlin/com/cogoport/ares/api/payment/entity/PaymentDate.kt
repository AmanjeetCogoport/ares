package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@MappedEntity
data class PaymentDate(
    var paymentNum: Long?,
    var transactionDate: Timestamp?,
)
