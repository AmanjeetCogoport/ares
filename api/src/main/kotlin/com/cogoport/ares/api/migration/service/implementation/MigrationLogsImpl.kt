package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.migration.constants.MigrationStatus
import com.cogoport.ares.api.migration.entity.MigrationLogs
import com.cogoport.ares.api.migration.repository.MigrationLogsRepository
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import jakarta.inject.Inject

class MigrationLogsImpl : MigrationLogService {

    @Inject lateinit var migrationLogsRepository: MigrationLogsRepository

    override suspend fun saveMigrationLogs(id: String?) {
        migrationLogsRepository.save(MigrationLogs(null, id, MigrationStatus.MIGRATED, null))
    }

    override suspend fun saveMigrationLogs(id: String?, ex: Exception?) {
        migrationLogsRepository.save(MigrationLogs(null, id, MigrationStatus.FAILED, ex?.stackTraceToString()))
    }
}
