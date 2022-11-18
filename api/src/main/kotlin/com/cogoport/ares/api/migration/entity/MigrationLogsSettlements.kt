package com.cogoport.ares.api.migration.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@MappedEntity(value = "settlements_migration_logs")
data class MigrationLogsSettlements(
    @field:Id @GeneratedValue var id: Long?,
    var sourceId: String?,
    var sourceValue: String?,
    var destinationId: String?,
    var destinationValue: String?,
    var ledgerCurrency: String?,
    var ledgerAmount: BigDecimal?,
    var accMode: String?,
    var status: String?,
    var errorMessage: String?,
    var migrationDate: Timestamp? = null,
)
