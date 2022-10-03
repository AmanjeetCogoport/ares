package com.cogoport.ares.api.common.controller

import com.cogoport.ares.api.common.service.interfaces.DownloadService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject
import java.io.File

@Controller("/download")
class DownloadController {

    @Inject
    lateinit var downloadService: DownloadService

    @Get
    suspend fun download(@QueryValue("id") id: String): MutableHttpResponse<File> {
        val file = downloadService.downloadDocument(id.toLong())
        return HttpResponse
            .ok(file)
            .header("Content-Disposition", "attachment; filename=" + file.name)
            .contentType(MediaType.MICROSOFT_EXCEL_OPEN_XML)
    }
}
