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
data class PaymentSequenceNumbers (
        @field:Id @GeneratedValue @NonNull var id: Int?,
        var sequence_type: String,
        var next_sequence_number: Long,
        @DateCreated
        var created_at: Timestamp?,
        @DateUpdated
        var updated_at: Timestamp?
        )