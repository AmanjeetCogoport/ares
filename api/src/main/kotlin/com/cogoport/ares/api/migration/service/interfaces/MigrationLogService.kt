package com.cogoport.ares.api.migration.service.interfaces

interface MigrationLogService {
    suspend fun saveMigrationLogs(id: String?)
    suspend fun saveMigrationLogs(id: String?, ex: Exception?)
}
