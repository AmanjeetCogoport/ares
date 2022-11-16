package com.cogoport.ares.api.migration.service.interfaces

interface PaymentMigrationWrapper {
    suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int
    suspend fun migrateJournalVoucher(startDate: String?, endDate: String?, jvNums: List<String>?): Int
    suspend fun migratePaymentsByDate(bpr: String, mode: String): Int
    suspend fun migratePaymentsByPaymentNum(paymentNums: List<String>): Int
}
