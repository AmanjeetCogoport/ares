package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord

interface SageService {
    suspend fun getPaymentDataFromSage(startDate: String?, endDate: String?): ArrayList<PaymentRecord>
    suspend fun getJournalVoucherFromSage(startDate: String?, endDate: String?): ArrayList<JournalVoucherRecord>
}
