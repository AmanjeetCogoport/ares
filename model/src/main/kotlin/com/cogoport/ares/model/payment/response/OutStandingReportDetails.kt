package com.cogoport.ares.model.payment.response

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected

@Introspected
@ExcelSheet(sheetName = "Sheet1")
data class OutStandingReportDetails(
    @ExcelColumn("Business Name")
    var organizationName: String?,
    @ExcelColumn("Invoice Number")
    var invoiceNumber: Long?,
    @ExcelColumn("Shipment ID")
    var jobNumber: String?,
    @ExcelColumn("Entity Code")
    var entityCode: String?,
    @ExcelColumn("Invoice Currency")
    var currency: String?,
    @ExcelColumn("Invoice Amount")
    var invoiceAmount: String?,
    @ExcelColumn("Open Invoice Amount")
    var openInvoiceAmount: String?,
    @ExcelColumn("Ledger Amount")
    var ledgerAmount: String?,
    @ExcelColumn("Invoice Date")
    var invoiceDate: String?,
    @ExcelColumn("Credit Days")
    var creditDays: String?,
    @ExcelColumn("Due Date")
    var dueDate: String?,
    @ExcelColumn("Days Overdue")
    var daysOverdue: String?,
    @ExcelColumn("Status")
    var status: String?,
    @ExcelColumn("Invoice Url")
    var invoiceUrl: String?,
    @ExcelColumn("ServiceType")
    var serviceType: String?,
    @ExcelColumn("BL")
    var bl: String?,
    @ExcelColumn("BL Document No.")
    var blDocNo: String?,
    @ExcelColumn("DO")
    var deliveryOrder: String?,
    @ExcelColumn("DO Document No.")
    var deliveryOrderDocumentNumber: String?,
    @ExcelColumn("Commercial Invoice")
    var commercialInvoice: String?,
    @ExcelColumn("Airway Bill Date")
    var airWayBillDate: String?,
    @ExcelColumn("ETA")
    var eta: String?,
    @ExcelColumn("ETD")
    var etd: String?,
    @ExcelColumn("Remarks")
    var remarks: String?,
)
