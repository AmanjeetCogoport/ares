package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.requests.SupplierPaymentStatsRequest
import com.cogoport.ares.api.payment.model.requests.SupplierReceivableRequest
import com.cogoport.ares.api.payment.model.response.SupplierReceivables
import com.cogoport.ares.api.payment.model.response.SupplierStatistics
import com.cogoport.ares.api.payment.service.interfaces.LSPDashboardService
import com.cogoport.ares.model.payment.request.LSPLedgerRequest
import com.cogoport.ares.model.payment.response.LSPLedgerResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/lsp")
class LSPDashboardController {

    @Inject
    lateinit var lspDashboardService: LSPDashboardService

    @Get("/receivable-stats{?request*}")
    suspend fun getReceivableStatsForSupplier(request: SupplierReceivableRequest): SupplierReceivables {
        return lspDashboardService.getReceivableStatsForSupplier(request)
    }

    @Get("/payment-stats{?request*}")
    suspend fun getOnAccountPaymentStatsForSupplier(request: SupplierPaymentStatsRequest): SupplierStatistics {
        return lspDashboardService.getPaymentStatsForSupplier(request)
    }

    @Get("/ledger{?request*}")
    suspend fun getLedgerForLSP(@Valid request: LSPLedgerRequest): LSPLedgerResponse {
        return lspDashboardService.getLSPLedger(request)
    }

    @Get("/ledger-download{?request*}")
    suspend fun downloadLedger(request: LSPLedgerRequest): String? {
        return lspDashboardService.downloadLSPLedger(request)
    }
}
