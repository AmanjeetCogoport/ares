package com.cogoport.ares.api.migration.service.interfaces

import com.cogoport.ares.api.migration.model.InvoiceDetails
import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentDetailsInfo
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.settlement.GlCodeMaster

interface SageService {
    suspend fun getPaymentDataFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): ArrayList<PaymentRecord>
    suspend fun getJournalVoucherFromSageCorrected(startDate: String?, endDate: String?, jvNums: String?, jvType: String?): ArrayList<JournalVoucherRecord>

    suspend fun migratePaymentsByDate(startDate: String?, endDate: String?, updatedAt: String?): ArrayList<PaymentRecord>
    suspend fun migratePaymentByPaymentNum(paymentNums: String): ArrayList<PaymentRecord>
    suspend fun getSettlementDataFromSage(startDate: String, endDate: String, source: String?, destination: String?): ArrayList<SettlementRecord>

    suspend fun getInvoicesPayLocDetails(startDate: String?, endDate: String?, updatedAt: String?, invoiceNumbers: String?): ArrayList<InvoiceDetails>

    suspend fun getBillPayLocDetails(startDate: String?, endDate: String?, updatedAt: String?): ArrayList<InvoiceDetails>

    suspend fun getJVDetails(startDate: String?, endDate: String?, jvNum: String?, sageJvId: String?): List<JVParentDetails>

    suspend fun getJournalVoucherFromSage(startDate: String?, endDate: String?, jvNums: String?): ArrayList<JournalVoucherRecord>

    suspend fun getPaymentsForScheduler(startDate: String, endDate: String): ArrayList<PaymentRecord>

    suspend fun getGLCode(): List<GlCodeMaster>

    suspend fun getNewPeriodRecord(startDate: String, endDate: String, bpr: String?, accMode: String): List<NewPeriodRecord>

    suspend fun getJVDetailsForScheduler(startDate: String?, endDate: String?, jvNum: String?): List<JVRecordsScheduler>

    suspend fun getPaymentPostSageInfo(paymentNumValue: String, entityCode: Long?, accMode: AccMode): PaymentDetailsInfo?

    suspend fun getSagePaymentNum(sageRefNumber: List<String>): ArrayList<SagePaymentNumMigrationResponse>

    suspend fun getMTCJVDetails(startDate: String?, endDate: String?): List<JVParentDetails>

    suspend fun getTDSJournalVoucherFromSageCorrected(startDate: String?, endDate: String?, jvNums: String?, jvType: String?): ArrayList<JournalVoucherRecord>

    suspend fun getTDSJVDetails(startDate: String?, endDate: String?, jvNum: String?, sageJvId: String?): List<JVParentDetails>
}
