package com.cogoport.ares.api.dunning.model.response

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
data class ExceptionExcelResp(
    @ExcelColumn("registration_number")
    var registrationNumber: String? = "",
    @ExcelColumn("customer_name")
    var customerName: String? = "",
    @ExcelColumn("error_reason")
    var errorReason: String? = ""
)
