package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord

interface PaymentMigration {
    suspend fun migratePayment(paymentRecord: PaymentRecord): Int
    suspend fun migarteJournalVoucher(journalVoucherRecord: JournalVoucherRecord): Int

    suspend fun migrateSettlements(settlementRecord: SettlementRecord)
    suspend fun updatePayment(paymentRecord: PaymentRecord)
}
