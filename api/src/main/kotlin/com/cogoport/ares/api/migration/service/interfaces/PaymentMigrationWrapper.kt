package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.model.common.PaymentStatusSyncMigrationReq
import com.cogoport.ares.model.common.TdsAmountReq
import com.cogoport.ares.model.settlement.GlCodeMaster

interface PaymentMigrationWrapper {
    suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int
    suspend fun migrateJournalVoucher(startDate: String?, endDate: String?, jvNums: List<String>?): Int
    suspend fun migratePaymentsByDate(startDate: String?, endDate: String?): Int
    suspend fun migratePaymentsByPaymentNum(paymentNums: List<String>): Int
    suspend fun migrateSettlementsWrapper(startDate: String, endDate: String, entries: Map<String, String>?): Int

    suspend fun updateUtilizationAmount(startDate: String?, endDate: String?, updatedAt: String?): Int

    suspend fun updateUtilizationAmountByPaymentNum(paymentNums: List<String>): Int
    suspend fun updateUtilizationForInvoice(startDate: String?, endDate: String?, updatedAt: String?, invoiceNumbers: List<String>?): Int
    suspend fun updateUtilizationForBill(startDate: String?, endDate: String?, updatedAt: String?): Int
    suspend fun migrateJournalVoucherRecordNew(startDate: String?, endDate: String?, jvNums: List<String>?, sageJvId: List<String>?): Int

    suspend fun migrateSettlementNumWrapper(ids: List<Long>)

    suspend fun migrateTdsAmount(req: List<TdsAmountReq>)

    suspend fun migrateGlAccount(): Int

    suspend fun createGLCode(request: GlCodeMaster)

    suspend fun upsertMigrateGLCode(request: GlCodeMaster)

    suspend fun migrateNewPR(startDate: String, endDate: String, bpr: String?, accMode: String)

    suspend fun migrateJVUtilization(startDate: String?, endDate: String?, jvNums: List<String>?): Int

    suspend fun removeDuplicatePayNums(payNumValues: List<String>): Int

    suspend fun paymentStatusSyncMigration(paymentStatusSyncMigrationReq: PaymentStatusSyncMigrationReq): Int

    suspend fun migrateSagePaymentNum(sageRefNumber: List<String>): Int

    suspend fun migrateMTCCVJV(startDate: String?, endDate: String?): Int

    suspend fun migrateJournalVoucherRecordTDS(startDate: String?, endDate: String?, jvNums: List<String>?, sageJvId: List<String>?): Int
}
