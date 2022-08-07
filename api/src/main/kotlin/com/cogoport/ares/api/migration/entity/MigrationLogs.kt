package com.cogoport.ares.api.migration.entity

import com.cogoport.ares.api.migration.constants.MigrationStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@MappedEntity(value = "migration_logs")
class MigrationLogs(
    @field:Id @GeneratedValue
    var id: Long?,
    val paymentId: Long? = null,
    val accUtilId: Long? = null,
    val paymentNum: String? = null,
    val currency: String? = null,
    val currencyAmount: BigDecimal? = null,
    val ledgerAmount: BigDecimal? = null,
    val bankPayAmount: BigDecimal? = null,
    val accountUtilCurrAmount: BigDecimal? = null,
    val accountUtilLedAmount: BigDecimal? = null,
    val status: MigrationStatus? = null,
    val errorMessage: String? = null,
    val createdAt: Timestamp
)
