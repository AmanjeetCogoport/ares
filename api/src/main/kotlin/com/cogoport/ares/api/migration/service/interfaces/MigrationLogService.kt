package com.cogoport.ares.api.migration.service.interfaces

interface MigrationLogService {
    suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, paymentNum: Long?)
    suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, ex: String?, paymentNum: Long?)
}
