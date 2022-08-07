package com.cogoport.ares.api.migration.service.interfaces

interface PaymentMigrationWrapper {
    suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?): Int
    suspend fun migrateJournalVoucher(startDate: String?, endDate: String?): Int
}
