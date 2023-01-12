package com.cogoport.ares.api.reports.services.interfaces

import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import java.io.File

interface ReportService {

    suspend fun outstandingReportDownload(request: SupplierOutstandingRequest): String

    suspend fun downloadOutstandingReport(id: Long): File
}
