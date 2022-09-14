package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.response.OverallStatsForCustomerResponse
import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse
import java.util.UUID

interface KamPaymentService {
    suspend fun getOverallStatsForKam(docValue: List<String>): OverallStatsForKamResponse

    suspend fun getOverdueInvoicesByDueDateForKam(docValue: List<String>): OverdueInvoicesResponse

    suspend fun getOverallStatsForCustomer(docValue: List<String>, custId: String): OverallStatsForCustomerResponse

    suspend fun getOverdueInvoicesByDueDateForCustomer(docValue: List<String>, custId: String): OverdueInvoicesResponse

}