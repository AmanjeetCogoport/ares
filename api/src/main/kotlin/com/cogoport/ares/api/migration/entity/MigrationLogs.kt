package com.cogoport.ares.api.migration.entity

import com.cogoport.ares.api.migration.constants.MigrationStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity(value = "migration_logs")
class MigrationLogs(
    @field:Id @GeneratedValue
    var id: Long?,
    val paymentId: Long? = null,
    val accUtilId: Long? = null,
    val paymentNum: Long? = null,
    val status: MigrationStatus? = null,
    val errorMessage: String? = null
)
