package com.cogoport.ares.api.settlement.model

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
class JobVoucherValidationModel(
    @ExcelColumn("parentId")
    var parentId: String? = null,
    @ExcelColumn("errorName")
    var errorName: String? = ""
)
