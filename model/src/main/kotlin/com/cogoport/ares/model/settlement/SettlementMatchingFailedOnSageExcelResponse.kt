package com.cogoport.ares.model.settlement

import com.cogoport.brahma.excel.annotations.ExcelColumn
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class SettlementMatchingFailedOnSageExcelResponse(
    @ExcelColumn(name = "Source Document Number")
    val sourceDocValue: String?,
    @ExcelColumn(name = "Destination Document Number")
    val destinationDocValue: String?,
    @ExcelColumn(name = "Source Sage Ref Number")
    val sourceSageRefNumber: String?,
    @ExcelColumn(name = "Destination Sage Ref Number")
    val destinationSageRefNumber: String?,
    @ExcelColumn(name = "Currency")
    val currency: String?,
    @ExcelColumn(name = "Ledger Amount")
    val amount: BigDecimal?,
    @ExcelColumn(name = "Ledger Currency")
    val ledCurrency: String?,
    @ExcelColumn(name = "Ledger Amount")
    val ledAmount: BigDecimal?,
    @ExcelColumn(name = "Request Param")
    val request: String?,
    @ExcelColumn(name = "Reasons")
    val response: String
)
