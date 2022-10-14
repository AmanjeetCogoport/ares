package com.cogoport.ares.api.migration.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime

@MappedEntity(value = "sage_precovid_bpr_numbers")
class DataC(
    @field:Id @GeneratedValue
    val id: Long,
    val bprId: String,
    val businessName: String,
    val isDeleted: Boolean,
    val createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    val updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now())
)
