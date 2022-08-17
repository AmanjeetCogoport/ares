package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord

interface PaymentMigration {
    suspend fun migratePayment(paymentRecord: PaymentRecord): Int
    suspend fun migarteJournalVoucher(journalVoucherRecord: JournalVoucherRecord): Int
}
