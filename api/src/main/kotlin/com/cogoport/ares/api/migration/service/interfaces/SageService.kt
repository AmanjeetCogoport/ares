package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord

interface SageService {
    suspend fun getPaymentDataFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): ArrayList<PaymentRecord>
    suspend fun getJournalVoucherFromSage(startDate: String?, endDate: String?, jvNums: String?): ArrayList<JournalVoucherRecord>

    suspend fun migratePaymentsByDate(startDate: String, endDate: String): ArrayList<PaymentRecord>

    suspend fun migratePaymentByPaymentNum(paymentNums: String): ArrayList<PaymentRecord>
}
