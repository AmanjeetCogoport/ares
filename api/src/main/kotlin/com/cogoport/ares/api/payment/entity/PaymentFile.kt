package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity("payment_files")
data class PaymentFile(
    @field:Id @GeneratedValue var id: Long?,
    var accMode: AccMode,
    var fileName: String,
    var fileUrl: String,
    var errorFileUrl: String?,
    var totalRecords: Int = 0,
    var successRecords: Int = 0,
    var createdBy: UUID,
    var updatedBy: UUID,
    @DateCreated var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
    @DateUpdated var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
)
