package com.cogoport.ares.api.settlement.model

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "ErrorSheet")
class ExcelValidationModel(
    @ExcelColumn("bpr")
    var bpr: String? = null,
    @ExcelColumn("errorName")
    var errorName: String? = ""
)
