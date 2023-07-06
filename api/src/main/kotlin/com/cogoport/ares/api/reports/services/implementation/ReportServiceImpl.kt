package com.cogoport.ares.api.reports.services.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AresDocument
import com.cogoport.ares.api.payment.repository.AresDocumentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.reports.services.interfaces.ReportService
import com.cogoport.ares.model.payment.SupplierOutstandingReportResponse
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Style
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Singleton
class ReportServiceImpl(
    private var s3Client: S3Client,
    private var aresDocumentRepository: AresDocumentRepository
) : ReportService {

    @Value("\${aws.s3.bucket}")
    private lateinit var s3Bucket: String

    @Inject
    lateinit var onAccountService: OnAccountService

    override suspend fun outstandingReportDownload(request: SupplierOutstandingRequest): String {
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
            reportList.add(getSupplierOutstandingReportResponse(it))
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
        return Hashids.encode(result.id!!)
    }

    override suspend fun downloadOutstandingReport(id: Long): File {
        val url = URLDecoder.decode(aresDocumentRepository.getSupplierOutstandingUrl(id), "UTF-8")
        val inputStreamFile = s3Client.download(url)
        val excelFile = File("/tmp/$inputStreamFile")
        Files.copy(inputStreamFile, excelFile.toPath())
        return excelFile
    }

    private fun <T : Any> writeIntoExcel(list: MutableList<T>, excelName: String): File {
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
            ).data(list).build()
        return file
    }
    private fun getSupplierOutstandingReportResponse(supplier: SupplierOutstandingDocument): SupplierOutstandingReportResponse {
        return SupplierOutstandingReportResponse(
            businessName = supplier.businessName,
            registrationNumber = supplier.registrationNumber,
            collectionPartyType = supplier.collectionPartyType,
            companyType = supplier.companyType,
            supplyAgentName = if (supplier.supplyAgent == null) null else supplier.supplyAgent!!.name,
            supplyAgentEmail = if (supplier.supplyAgent == null) null else supplier.supplyAgent!!.email,
            supplyAgentMobileCountryCode = if (supplier.supplyAgent == null) null else supplier.supplyAgent!!.mobileCountryCode,
            supplyAgentMobileNumber = if (supplier.supplyAgent == null) null else supplier.supplyAgent!!.mobileNumber,
            sageId = supplier.sageId,
            organizationSerialId = supplier.organizationSerialId,
            countryCode = supplier.countryCode,
            category = supplier.category,
            serialId = supplier.serialId,
            creditDays = if (supplier.creditDays == null) 0 else supplier.creditDays?.toLong(),
            openInvoiceCount = supplier.openInvoiceCount,
            openInvoiceAmount = supplier.openInvoiceLedgerAmount,
            totalOutstandingAmount = supplier.totalOutstandingInvoiceLedgerAmount,
            totalOutstandingCount = supplier.totalOutstandingInvoiceCount,
            onAccountPaymentAmount = supplier.onAccountPaymentInvoiceLedgerAmount,
            onAccountPaymentInvoiceCount = supplier.onAccountPaymentInvoiceCount,
            notDueCount = supplier.notDueCount,
            todayCount = supplier.todayCount,
            thirtyCount = supplier.thirtyCount,
            sixtyCount = supplier.sixtyCount,
            ninetyCount = supplier.ninetyCount,
            oneEightyCount = supplier.oneEightyCount,
            threeSixtyFiveCount = supplier.threeSixtyFiveCount,
            threeSixtyFivePlusCount = supplier.threeSixtyFivePlusCount,
            notDueAmount = supplier.notDueAmount,
            todayAmount = supplier.todayAmount,
            thirtyAmount = supplier.thirtyAmount,
            sixtyAmount = supplier.sixtyAmount,
            ninetyAmount = supplier.ninetyAmount,
            oneEightyAmount = supplier.oneEightyAmount,
            threeSixtyFiveAmount = supplier.threeSixtyFiveAmount,
            threeSixtyFivePlusAmount = supplier.threeSixtyFivePlusAmount
        )
    }

    override suspend fun getARLedgerReport(req: LedgerSummaryRequest): String {
        val report = onAccountService.getARLedgerOrganizationAndEntityWise(req)
        val startDate = Date(req.startDate?.time!!).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Date(req.endDate?.time!!).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val orgName = req.orgName?.replace(" ", "_")
        val excelName = "AR_Ledger_${orgName}_from_${startDate}_to_$endDate"
        val file = writeIntoExcel(report.toMutableList(), excelName)
        val url = s3Client.upload(s3Bucket, "$excelName.xlsx", file).toString()
        val result = aresDocumentRepository.save(
            AresDocument(
                id = null,
                documentUrl = url,
                documentName = "AR_Ledger_Report",
                documentType = url.substringAfterLast('.'),
                uploadedBy = req.requestedBy!!,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now())
            )
        )
        return Hashids.encode(result.id!!)
    }
}
