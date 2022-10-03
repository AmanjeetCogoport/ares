package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity("ares_documents")
data class AresDocument(
    @field:Id @GeneratedValue var id: Long? = 0L,
    var documentUrl: String,
    var documentName: String,
    var documentType: String,
    var uploadedBy: UUID,
    @DateCreated var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    @DateUpdated var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis())
)
