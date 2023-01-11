package com.cogoport.ares.api.reports.services.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AresDocument
import com.cogoport.ares.api.payment.repository.AresDocumentRepository
import com.cogoport.ares.api.reports.services.interfaces.ReportService
import com.cogoport.ares.model.payment.SupplierOutstandingReportResponse
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Style
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Singleton
class ReportServiceImpl(
    private var s3Client: S3Client,
    private var aresDocumentRepository: AresDocumentRepository
) : ReportService {

    @Value("\${aws.s3.bucket}")
    private lateinit var s3Bucket: String

    override suspend fun outstandingReportDownload(request: SupplierOutstandingRequest): File {
        request.limit = AresConstants.LIMIT
        var index: String = AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX

        if (request.flag != "overall") {
            index = "supplier_outstanding_${request.flag}"
        }

        val response = OpenSearchClient().listSupplierOutstanding(request, index)
        var list: List<SupplierOutstandingDocument>? = listOf()
        if (!response?.hits()?.hits().isNullOrEmpty()) {
            list = response?.hits()?.hits()?.map { it.source()!! }
        }
        if (list.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1002, "No Supplier Found")
        }
        val reportList: MutableList<SupplierOutstandingReportResponse> = mutableListOf()
        list.forEach {
            val supplier = SupplierOutstandingReportResponse(
                organizationId = it.organizationId,
                selfOrganizationId = it.selfOrganizationId,
                businessName = it.businessName,
                registrationNumber = it.registrationNumber,
                collectionPartyType = "",
                companyType = it.companyType,
                supplyAgentId = if (it.supplyAgent == null) null else it.supplyAgent!!.id.toString(),
                supplyAgentName = if (it.supplyAgent == null) null else it.supplyAgent!!.name,
                supplyAgentEmail = if (it.supplyAgent == null) null else it.supplyAgent!!.email,
                supplyAgentMobileCountryCode = if (it.supplyAgent == null) null else it.supplyAgent!!.mobileCountryCode,
                supplyAgentMobileNumber = if (it.supplyAgent == null) null else it.supplyAgent!!.mobileNumber,
                sageId = it.sageId,
                countryId = it.countryId,
                countryCode = it.countryCode,
                category = "",
                serialId = it.serialId,
                organizationSerialId = it.organizationSerialId,
                creditDays = if (it.creditDays == null) "" else it.creditDays!!.toString(),
                openInvoice = it.openInvoiceCount.toString(),
                totalOutstanding = it.totalOutstandingInvoiceLedgerAmount.toString(),
                onAccountPayment = it.onAccountPaymentInvoiceLedgerAmount.toString(),
                notDueAmount = it.notDueAmount.toString(),
                todayAmount = it.todayAmount.toString(),
                thirtyAmount = it.thirtyAmount.toString(),
                sixtyAmount = it.sixtyAmount.toString(),
                ninetyAmount = it.ninetyAmount.toString(),
                oneEightyAmount = it.oneEightyAmount.toString(),
                threeSixtyFiveAmount = it.threeSixtyFiveAmount.toString(),
                threeSixtyFivePlusAmount = it.threeSixtyFivePlusAmount.toString()
            )
            reportList.add(supplier)
        }
        val excelName = "Supplier_Outstanding" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss")) + "_" + reportList.size
        val file = writeIntoExcel(reportList, excelName)
        var url = s3Client.upload(s3Bucket, "$excelName.xlsx", file).toString()
        val result = aresDocumentRepository.save(
            AresDocument(
                id = null,
                documentUrl = url,
                documentName = "supplier_outstanding_report",
                documentType = url.substringAfterLast('.'),
                uploadedBy = request.performedBy!!,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now())
            )
        )
        url = URLDecoder.decode(url, "UTF-8")

        val inputStreamFile = s3Client.download(url)
        val excelFile = File("/tmp/Supplier_Outstanding_Report_${Hashids.encode(result.id!!)}_${Instant.now()}.xlsx")
        Files.copy(inputStreamFile.inputStream(), excelFile.toPath())
        return excelFile
    }

    private fun writeIntoExcel(outstandingList: MutableList<SupplierOutstandingReportResponse>, excelName: String): File {
        val file = ExcelSheetBuilder.Builder()
            .filename(excelName)
            .sheetName("")
            .headerStyle( // Header style for all columns if you want to change the style of the individual header, you can pass style in the header object
                Style(
                    fontStyle = FontStyle.BOLD,
                    fontSize = 12,
                    fontColor = Color.BLACK,
                    background = Color.YELLOW
                )
            ).data(outstandingList).build()
        return file
    }
}
