package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.migration.constants.MigrationStatus
import com.cogoport.ares.api.migration.entity.MigrationLogs
import com.cogoport.ares.api.migration.repository.MigrationLogsRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import jakarta.inject.Inject

class MigrationLogsImpl : MigrationLogService {

    @Inject lateinit var migrationLogsRepository: MigrationLogsRepository

    override suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, paymentNum: Long?) {
        if (paymentId == null && accUtilId == null) {
            migrationLogsRepository.save(MigrationLogs(null, null, null, paymentNum, MigrationStatus.NOT_MIGRATED, null))
        }
        migrationLogsRepository.save(MigrationLogs(null, paymentId, accUtilId, paymentNum, MigrationStatus.MIGRATED, null))
    }

    override suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, ex: String?, paymentNum: Long?) {
        migrationLogsRepository.save(MigrationLogs(null, paymentId, accUtilId, paymentNum, MigrationStatus.FAILED, ex))
    }
}
