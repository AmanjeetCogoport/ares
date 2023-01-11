package com.cogoport.ares.api.reports.controller

import com.cogoport.ares.api.reports.services.interfaces.ReportService
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.io.File
import javax.validation.Valid

@Validated
@Controller("/report")
class ReportController {
    @Inject
    lateinit var reportService: ReportService

    @Get("/download{?request*}")
    suspend fun supplierOutstandingReportDownload(@Valid request: SupplierOutstandingRequest): MutableHttpResponse<File>? {
        val file: File = reportService.outstandingReportDownload(request)
        return HttpResponse.ok(file).header("Content-Disposition", "filename=${file.name}")
    }
}
