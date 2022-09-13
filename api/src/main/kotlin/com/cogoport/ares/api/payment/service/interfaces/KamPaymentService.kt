package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse

interface KamPaymentService {
    suspend fun getProformaInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse

    suspend fun getDueForPaymentForKam(proformaIds: List<String>): OverallStatsForKamResponse

    suspend fun getOverdueInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse

    suspend fun getTotalReceivablesForKam(proformaIds: List<String>): OverallStatsForKamResponse

    suspend fun getOverdueInvoicesByDueDateForKam(proformaIds: List<String>): OverdueInvoicesResponse

}