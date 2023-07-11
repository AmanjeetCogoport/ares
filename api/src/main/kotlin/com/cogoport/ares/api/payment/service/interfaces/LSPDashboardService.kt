package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.requests.SupplierPaymentStatsRequest
import com.cogoport.ares.api.payment.model.requests.SupplierReceivableRequest
import com.cogoport.ares.api.payment.model.response.SupplierReceivables
import com.cogoport.ares.api.payment.model.response.SupplierStatistics
import com.cogoport.ares.model.payment.request.LSPLedgerRequest
import com.cogoport.ares.model.payment.response.LSPLedgerResponse

interface LSPDashboardService {
    suspend fun getReceivableStatsForSupplier(request: SupplierReceivableRequest): SupplierReceivables

    suspend fun getPaymentStatsForSupplier(request: SupplierPaymentStatsRequest): SupplierStatistics

    suspend fun getLSPLedger(request: LSPLedgerRequest): LSPLedgerResponse

    suspend fun downloadLSPLedger(request: LSPLedgerRequest): String?
}
