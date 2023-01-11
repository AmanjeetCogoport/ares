package com.cogoport.ares.model.payment

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
data class SupplierOutstandingReportResponse(
    @ExcelColumn("Trade Party Serial Id")
    var serialId: String?,
    @ExcelColumn("Business Name")
    var businessName: String?,
    @ExcelColumn("Sage Id")
    var sageId: String?,
    @ExcelColumn("Registration Number")
    var registrationNumber: String?,
    @ExcelColumn("Collection Party Type")
    var collectionPartyType: List<String?>?,
    @ExcelColumn("Company Type")
    var companyType: String?,
    @ExcelColumn("Supply Agent Name")
    var supplyAgentName: String?,
    @ExcelColumn("Supply Agent Email")
    var supplyAgentEmail: String?,
    @ExcelColumn("Agent Mobile Country Code")
    var supplyAgentMobileCountryCode: String?,
    @ExcelColumn("Supply Agent Mobile Number")
    var supplyAgentMobileNumber: String?,
    @ExcelColumn("Country Id")
    var countryId: String?,
    @ExcelColumn("Country Code")
    var countryCode: String?,
    @ExcelColumn("Category")
    var category: List<String?>?,
    @ExcelColumn("Credit Days")
    var creditDays: Long?,
    @ExcelColumn("Open Invoice Count")
    var openInvoiceCount: Int?,
    @ExcelColumn("Open Invoice Amount")
    var openInvoiceAmount: BigDecimal?,
    @ExcelColumn("On Account Invoice Count")
    var onAccountPaymentInvoiceCount: Int?,
    @ExcelColumn("On Account Payment Amount")
    var onAccountPaymentAmount: BigDecimal?,
    @ExcelColumn("Total Outstanding Count")
    var totalOutstandingCount: Int?,
    @ExcelColumn("Total Outstanding Amount")
    var totalOutstandingAmount: BigDecimal?,
    @ExcelColumn("Not Due Count")
    var notDueCount: Int?,
    @ExcelColumn("Not Due Amount")
    var notDueAmount: BigDecimal?,
    @ExcelColumn("Today Count")
    var todayCount: Int?,
    @ExcelColumn("Today Amount")
    var todayAmount: BigDecimal?,
    @ExcelColumn("0 - 30 Days Due Count")
    var thirtyCount: Int?,
    @ExcelColumn("0 - 30 Days Due Amount")
    var thirtyAmount: BigDecimal?,
    @ExcelColumn("31 - 60 Days Due Count")
    var sixtyCount: Int?,
    @ExcelColumn("31 - 60 Days Due Amount")
    var sixtyAmount: BigDecimal?,
    @ExcelColumn("61-90 Days Due Count")
    var ninetyCount: Int?,
    @ExcelColumn("61-90 Days Due Amount")
    var ninetyAmount: BigDecimal?,
    @ExcelColumn("91-180 Days Due Count")
    var oneEightyCount: Int?,
    @ExcelColumn("91-180 Days Due Amount")
    var oneEightyAmount: BigDecimal?,
    @ExcelColumn("181-365 Days Due Count")
    var threeSixtyFiveCount: Int?,
    @ExcelColumn("181-365 Days Due Amount")
    var threeSixtyFiveAmount: BigDecimal?,
    @ExcelColumn("365+ Days Due Count")
    var threeSixtyFivePlusCount: Int?,
    @ExcelColumn("365+ Days Due Amount")
    var threeSixtyFivePlusAmount: BigDecimal?
)
