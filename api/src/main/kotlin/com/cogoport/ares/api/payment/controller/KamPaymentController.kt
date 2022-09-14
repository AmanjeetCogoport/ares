package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.KamPaymentService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.response.OverallStatsForCustomerResponse
import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/kam-payment")
class KamPaymentController {
    @Inject
    lateinit var kamPaymentService: KamPaymentService

    @Get("/overallStatsForKam")
    suspend fun getOverallStatsForKam(@QueryValue("docValue") docValue: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(kamPaymentService.getOverallStatsForKam(docValue))
    }

    @Get("/overdueInvoiceByDueDate")
    suspend fun getOverdueInvoicesByDueDateForKam(@QueryValue("docValue") docValue: List<String>): OverdueInvoicesResponse {
        return Response<OverdueInvoicesResponse>().ok(kamPaymentService.getOverdueInvoicesByDueDateForKam(docValue))
    }

    @Get("/overallStatsForCustomer")
    suspend fun getOverallStatsForCustomer(@QueryValue("docValue") docValue: List<String>,
                                           @QueryValue("custId")custId: String): OverallStatsForCustomerResponse {
        return Response<OverallStatsForCustomerResponse>().ok(kamPaymentService.getOverallStatsForCustomer(docValue, custId))
    }

    @Get("/overdueInvoiceByDueDateForCustomer")
    suspend fun getOverdueInvoicesByDueDateForCustomer(@QueryValue("docValue") docValue: List<String>,
                                                       @QueryValue("custId")custId: String): OverdueInvoicesResponse {
        return Response<OverdueInvoicesResponse>().ok(kamPaymentService.getOverdueInvoicesByDueDateForCustomer(docValue, custId))
    }

}