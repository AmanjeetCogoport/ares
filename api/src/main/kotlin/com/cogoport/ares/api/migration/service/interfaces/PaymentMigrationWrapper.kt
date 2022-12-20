package com.cogoport.ares.api.migration.service.interfaces

interface PaymentMigrationWrapper {
    suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int
    suspend fun migrateJournalVoucher(startDate: String?, endDate: String?, jvNums: List<String>?): Int
    suspend fun migratePaymentsByDate(startDate: String?, endDate: String?): Int
    suspend fun migratePaymentsByPaymentNum(paymentNums: List<String>): Int
    suspend fun migrateSettlementsWrapper(startDate: String, endDate: String, entries: Map<String, String>?): Int

    suspend fun updateUtilizationAmount(startDate: String?, endDate: String?, updatedAt: String?): Int

    suspend fun updateUtilizationAmountByPaymentNum(paymentNums: List<String>): Int
    suspend fun updateUtilizationForInvoice(startDate: String?, endDate: String?, updatedAt: String?): Int
    suspend fun updateUtilizationForBill(startDate: String?, endDate: String?, updatedAt: String?): Int
}
