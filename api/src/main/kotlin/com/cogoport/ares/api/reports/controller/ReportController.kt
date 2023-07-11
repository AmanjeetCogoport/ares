package com.cogoport.ares.api.reports.controller

import com.cogoport.ares.api.reports.services.interfaces.ReportService
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.brahma.hashids.Hashids
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

    @Get("/supplier-outstanding{?request*}")
    suspend fun supplierOutstandingReportDownload(@Valid request: SupplierOutstandingRequest): String {
        return reportService.outstandingReportDownload(request)
    }
    @Get("/download/{id}")
    suspend fun downloadReport(@PathVariable("id") id: String): MutableHttpResponse<File>? {
        val file: File = reportService.downloadOutstandingReport(Hashids.decode(id)[0])
        return HttpResponse.ok(file).header("Content-Disposition", "filename=${file.name}")
    }
    @Get("/ar-ledger{?request*}")
    suspend fun getARLedgerReport(@Valid request: LedgerSummaryRequest): String {
        return reportService.getARLedgerReport(request)
    }
}
