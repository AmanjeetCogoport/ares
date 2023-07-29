package com.cogoport.ares.api.reports.services.implementation

import builders.dsl.spreadsheet.api.Keywords
import builders.dsl.spreadsheet.builder.api.CellDefinition
import builders.dsl.spreadsheet.builder.api.RowDefinition
import builders.dsl.spreadsheet.builder.api.SheetDefinition
import builders.dsl.spreadsheet.builder.api.WorkbookDefinition
import builders.dsl.spreadsheet.builder.poi.PoiSpreadsheetBuilder
import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.AresConstants.LEDGER_CURRENCY
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
import com.cogoport.ares.model.payment.response.ARLedgerResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Header
import com.cogoport.brahma.excel.model.Style
import com.cogoport.brahma.excel.utils.Constants
import com.cogoport.brahma.excel.utils.applyStyle
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.lang.reflect.Field
import java.math.RoundingMode
import java.net.URLDecoder
import java.nio.file.Files
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import builders.dsl.spreadsheet.api.FontStyle as ExcelFont

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
        val excelFile = File("/tmp/${url.substringAfterLast("/").substringBefore(".xlsx")}_${Instant.now()}.xlsx")
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
        var reportHeader: MutableMap<String, String?> = mutableMapOf()

        val report = onAccountService.getARLedgerOrganizationAndEntityWise(req)
        val startDate = Date(req.startDate?.time!!).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Date(req.endDate?.time!!).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val orgName = req.orgName?.replace(" ", "_")

        reportHeader["Cogo Entity"] = req.entityCodes.toString()
        reportHeader["Company Name"] = req.orgName
        reportHeader["Start Date"] = startDate.toString()
        reportHeader["End Date"] = endDate.toString()
        reportHeader["Document Date"] = LocalDate.now().toString()
        reportHeader["Ledger Currency"] = LEDGER_CURRENCY[req.entityCodes?.get(0)]
        reportHeader["Opening Account Balance"] = (report[0].debitBalance - report[0].creditBalance).setScale(2, RoundingMode.UP).toString()
        reportHeader["Closing Account Balance"] = (report[report.lastIndex].debitBalance - report[report.lastIndex].creditBalance).setScale(2, RoundingMode.UP).toString()

        val excelName = "AR_Ledger_Report_${orgName}_from_${startDate}_to_$endDate"
        var file = writeHeaderIntoExcel(reportHeader, report, excelName)
        val url = s3Client.upload(s3Bucket, "$excelName.xlsx", file).toString()
        aresDocumentRepository.save(
            AresDocument(
                id = null,
                documentUrl = url,
                documentName = excelName,
                documentType = url.substringAfterLast('.'),
                uploadedBy = req.requestedBy!!,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now())
            )
        )
        return url
    }

    private fun writeHeaderIntoExcel(
        reportHeader: MutableMap<String, String?>,
        report: List<ARLedgerResponse>,
        excelName: String
    ): File {
        val file: File = File.createTempFile(excelName, Constants.EXCEL_FILE_EXTENSION_WITH_DOT)

        val sheet = report.first()
        val fields = getAllFields(arrayListOf(), sheet.javaClass)

        val headers = sheet.let {
            return@let fields.map {
                it.isAccessible = true
                val excelColumn = it.getAnnotation(ExcelColumn::class.java)
                Header(excelColumn.name)
            }
        }

        val values = report.map { obj ->
            return@map fields.map {
                it.isAccessible = true
                it.get(obj)
            }
        }

        PoiSpreadsheetBuilder.create(file).build { w: WorkbookDefinition ->
            w.sheet("") { s: SheetDefinition ->
                s.row()
                reportHeader.forEach { (k, v) ->
                    s.row { r ->
                        r.cell { c ->
                            c.value(k)
                            c.style { st -> st.font { f -> f.style(ExcelFont.BOLD) } }
                        }
                        r.cell(v)
                    }
                    if (k == AresConstants.DOCUMENT_DATE) {
                        s.row()
                    }
                }
                s.row()

                s.row { r: RowDefinition ->
                    headers.forEach { header ->
                        r.cell { cd: CellDefinition ->
                            cd.value(header.name)
                            cd.width(30.00)
                            cd.applyStyle(
                                Style(
                                    fontStyle = FontStyle.BOLD,
                                    fontSize = 12,
                                    fontColor = Color.BLACK,
                                    background = Color.YELLOW
                                )
                            )
                        }
                    }
                }

                values.forEach { row ->
                    s.row { r: RowDefinition ->
                        if (row[2] == AresConstants.OPENING_BALANCE || row[2] == AresConstants.CLOSING_BALANCE) {
                            r.cell { c ->
                                c.value(row[2])
                                c.colspan(5)
                                c.style { st ->
                                    st.font { f ->
                                        f.style(ExcelFont.BOLD)
                                        st.align(Keywords.VerticalAlignment.CENTER, Keywords.HorizontalAlignment.CENTER)
                                    }
                                }
                            }
                            for (i in 5..row.lastIndex) {
                                r.cell { c ->
                                    c.value(row[i])
                                    c.style { st -> st.font { f -> f.style(ExcelFont.BOLD) } }
                                }
                            }
                        } else {
                            row.forEach { r.cell(it) }
                        }
                    }
                }
            }
        }

        return file
    }

    private fun <T> getAllFields(fields: ArrayList<Field>, javaClass: Class<T>): List<Field> {
        fields.addAll(javaClass.declaredFields.filter { it.isAnnotationPresent(ExcelColumn::class.java) })
        if (javaClass.superclass != null) {
            getAllFields(fields, javaClass.superclass)
        }
        return fields
    }
}
