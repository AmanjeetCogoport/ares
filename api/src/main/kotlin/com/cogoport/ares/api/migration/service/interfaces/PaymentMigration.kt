package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse

interface PaymentMigration {
    suspend fun migratePayment(paymentRecord: PaymentRecord): Int
    suspend fun migrateJournalVoucher(journalVoucherRecord: JournalVoucherRecord, parentJvId: Long)

    suspend fun migrateSettlements(settlementRecord: SettlementRecord)
    suspend fun updatePayment(payLocUpdateRequest: PayLocUpdateRequest)

    suspend fun migrateJV(journalVoucherRecord: JVParentDetails)
    suspend fun migrateSettlementNum(ids: Long)

    suspend fun migrateNewPeriodRecords(records: NewPeriodRecord)

    suspend fun migrateJVUtilization(record: JVRecordsScheduler)

    suspend fun migrateSagePaymentNum(request: SagePaymentNumMigrationResponse)

    suspend fun partialPaymentMismatchDocument(documentNo: Long)

    suspend fun migrateTDSJV(journalVoucherRecord: JVParentDetails)

    suspend fun migrateAdminJV(journalVoucherRecord: JVParentDetails)

    suspend fun mismatchAmount(id: Long)

}
