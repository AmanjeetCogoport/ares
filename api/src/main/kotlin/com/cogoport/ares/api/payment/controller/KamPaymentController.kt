package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.KamPaymentService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/kam-payment")
class KamPaymentController {
    @Inject
    lateinit var kamPaymentService: KamPaymentService

    @Get("/proforma-invoices")
    suspend fun getProformaInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(kamPaymentService.getProformaInvoicesForKam(proformaIds))
    }

    @Get("/duePayment")
    suspend fun getDuePaymentForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(kamPaymentService.getDueForPaymentForKam(proformaIds))
    }

    @Get("/overdueInvoices")
    suspend fun getOverdueInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(kamPaymentService.getOverdueInvoicesForKam(proformaIds))
    }

    @Get("/totalReceivables")
    suspend fun getTotalReceivablesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(kamPaymentService.getTotalReceivablesForKam(proformaIds))
    }

    @Get("/overdueInvoiceByDueDate")
    suspend fun getOverdueInvoicesByDueDateForKam(proformaIds: List<String>): OverdueInvoicesResponse {
        return Response<OverdueInvoicesResponse>().ok(kamPaymentService.getOverdueInvoicesByDueDateForKam(proformaIds))
    }
}