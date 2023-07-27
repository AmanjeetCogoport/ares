package com.cogoport.ares.api.utils

import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Style
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ExcelUtils {

    fun writeIntoExcel(dataList: List<Any>, excelName: String, sheetName: String): File {
        val file = ExcelSheetBuilder.Builder()
            .filename(excelName) // Filename without extension
            .sheetName(sheetName)
            .headerStyle( // Header style for all columns if you want to change the style of the individual header, you can pass style in the header object
                Style(
                    fontStyle = FontStyle.BOLD,
                    fontSize = 12,
                    fontColor = Color.BLACK,
                    background = Color.YELLOW
                )
            ).data(dataList).build()
        return file
    }

    fun downloadExcelFile(fileUrl: String): File {
        val httpClient: HttpClient = HttpClient.newBuilder().build()

        val httpRequest: HttpRequest = HttpRequest
            .newBuilder()
            .uri(URI(fileUrl))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = httpClient
            .send(httpRequest) { HttpResponse.BodySubscribers.ofInputStream() }
        val fileNme = fileUrl.split("/").last()
        val file = File(fileNme)
        val inp: InputStream = ByteArrayInputStream(response.body().readAllBytes())
        val path = Paths.get(file.path) as Path
        Files.copy(inp, path, StandardCopyOption.REPLACE_EXISTING)
        return file
    }
}
