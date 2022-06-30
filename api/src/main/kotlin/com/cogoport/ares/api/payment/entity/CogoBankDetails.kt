package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "cogo_bank_details")
data class CogoBankDetails(
    @field:Id @GeneratedValue var id: Int?,
    var bankId: UUID,
    var accountNo: String,
    var bankName: String,
    var entityCode: Int,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis())
)
