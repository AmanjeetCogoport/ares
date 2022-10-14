package com.cogoport.ares.api.migration.model

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import java.sql.Timestamp
import java.time.LocalDateTime

// @MappedEntity(value = "sage_precovid_bpr_numbers")
data class Data(
    @field:Id @GeneratedValue
    val id: Long?,
    val bprId: String,
    val businessName: String,
    val isDeleted: Boolean,
    val createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    val updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now())
)
