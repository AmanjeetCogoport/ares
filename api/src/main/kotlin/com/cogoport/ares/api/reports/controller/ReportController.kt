package com.cogoport.ares.api.reports.controller

import com.cogoport.ares.api.reports.services.interfaces.ReportService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.model.payment.request.ARLedgerRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import com.cogoport.brahma.hashids.Hashids
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.io.File
import javax.validation.Valid

@Validated
@Controller("/report")
class ReportController {
    @Inject
    lateinit var reportService: ReportService

    @Inject
    lateinit var util: Util

    @Auth
    @Get("/supplier-outstanding{?request*}")
    suspend fun supplierOutstandingReportDownload(@Valid request: SupplierOutstandingRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): String {
        request.flag = util.getCogoEntityCode(user?.filters?.get("partner_id")) ?: request.flag
        return reportService.outstandingReportDownload(request)
    }
    @Get("/download/{id}")
    suspend fun downloadReport(@PathVariable("id") id: String): MutableHttpResponse<File>? {
        val file: File = reportService.downloadOutstandingReport(Hashids.decode(id)[0])
        return HttpResponse.ok(file).header("Content-Disposition", "filename=${file.name}")
    }
    @Get("/ar-ledger{?request*}")
    suspend fun getARLedgerReport(@Valid request: ARLedgerRequest): String {
        return reportService.getARLedgerReport(request)
    }
}
