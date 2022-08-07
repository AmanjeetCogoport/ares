package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.migration.constants.MigrationStatus
import com.cogoport.ares.api.migration.entity.MigrationLogs
import com.cogoport.ares.api.migration.repository.MigrationLogsRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import jakarta.inject.Inject
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

class MigrationLogsImpl : MigrationLogService {

    @Inject lateinit var migrationLogsRepository: MigrationLogsRepository

    override suspend fun saveMigrationLogs(
        paymentId: Long?,
        accUtilId: Long?,
        paymentNum: String?,
        currency: String?,
        currencyAmount: BigDecimal?,
        ledgerAmount: BigDecimal?,
        bankPayAmount: BigDecimal?,
        accountUtilCurrAmount: BigDecimal?,
        accountUtilLedAmount: BigDecimal?
    ) {
        if (paymentId == null && accUtilId == null) {
            migrationLogsRepository.save(
                MigrationLogs(
                    null, paymentId, accUtilId, paymentNum, currency, currencyAmount, ledgerAmount, bankPayAmount,
                    accountUtilCurrAmount, accountUtilLedAmount, MigrationStatus.NOT_MIGRATED, null, Timestamp.from(Instant.now())
                )
            )
        }
        migrationLogsRepository.save(
            MigrationLogs(
                null, paymentId, accUtilId, paymentNum, currency, null, null, null,
                null, null, MigrationStatus.MIGRATED, null, Timestamp.from(Instant.now())
            )
        )
    }

    override suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, ex: String?, paymentNum: String?) {
        migrationLogsRepository.save(
            MigrationLogs(
                null, null, null, paymentNum, null, null, null, null,
                null, null, MigrationStatus.FAILED, null, Timestamp.from(Instant.now())
            )
        )
    }
}
