package com.cogoport.ares.api.migration.entity

import com.cogoport.ares.api.migration.constants.MigrationStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity(value = "payments")
class MigrationLogs(
        @field:Id @GeneratedValue
        var id: Long?,
        val paymentId: String? = null,
        val status: MigrationStatus? = null,
        val errorMessage: String? = null
)
