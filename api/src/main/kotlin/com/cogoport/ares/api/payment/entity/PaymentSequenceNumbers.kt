package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity("payment_sequence_numbers")
data class PaymentSequenceNumbers(
    @field:Id @GeneratedValue @NonNull var id: Int?,
    var sequenceType: String,
    var nextSequenceNumber: Long,
    @DateCreated
    var createdAt: Timestamp?,
    @DateUpdated
    var updatedAt: Timestamp?
)
