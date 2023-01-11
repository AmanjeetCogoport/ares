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
    @ExcelColumn("Serial Id")
    var organizationSerialId: String?,
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
    @ExcelColumn("Country Code")
    var countryCode: String?,
    @ExcelColumn("Category")
    var category: List<String?>?,
    @ExcelColumn("Credit Days")
    var creditDays: Long? = 0,
    @ExcelColumn("Open Invoice Count")
    var openInvoiceCount: Int? = 0,
    @ExcelColumn("Open Invoice Amount")
    var openInvoiceAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("On Account Invoice Count")
    var onAccountPaymentInvoiceCount: Int? = 0,
    @ExcelColumn("On Account Payment Amount")
    var onAccountPaymentAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("Total Outstanding Count")
    var totalOutstandingCount: Int? = 0,
    @ExcelColumn("Total Outstanding Amount")
    var totalOutstandingAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("Not Due Count")
    var notDueCount: Int? = 0,
    @ExcelColumn("Not Due Amount")
    var notDueAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("Today Count")
    var todayCount: Int? = 0,
    @ExcelColumn("Today Amount")
    var todayAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("0 - 30 Days Due Count")
    var thirtyCount: Int? = 0,
    @ExcelColumn("0 - 30 Days Due Amount")
    var thirtyAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("31 - 60 Days Due Count")
    var sixtyCount: Int? = 0,
    @ExcelColumn("31 - 60 Days Due Amount")
    var sixtyAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("61-90 Days Due Count")
    var ninetyCount: Int? = 0,
    @ExcelColumn("61-90 Days Due Amount")
    var ninetyAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("91-180 Days Due Count")
    var oneEightyCount: Int? = 0,
    @ExcelColumn("91-180 Days Due Amount")
    var oneEightyAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("181-365 Days Due Count")
    var threeSixtyFiveCount: Int? = 0,
    @ExcelColumn("181-365 Days Due Amount")
    var threeSixtyFiveAmount: BigDecimal? = BigDecimal.ZERO,
    @ExcelColumn("365+ Days Due Count")
    var threeSixtyFivePlusCount: Int? = 0,
    @ExcelColumn("365+ Days Due Amount")
    var threeSixtyFivePlusAmount: BigDecimal? = BigDecimal.ZERO
)
