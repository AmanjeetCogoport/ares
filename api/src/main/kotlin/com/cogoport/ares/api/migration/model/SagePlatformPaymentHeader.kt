package com.cogoport.ares.api.migration.model

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
data class SagePlatformPaymentHeader(
    @ExcelColumn("Payment Number on platform") var paymentNumValueAtPlatform: String?,
    @ExcelColumn("Payment Number on Sage") var paymentNumValueAtSage: String?,
    @ExcelColumn("BPR on platform") var sageOrganizationIdAtPlatform: String?,
    @ExcelColumn("BPR on Sage") var sageOrganizationIdAtSage: String?,
    @ExcelColumn("BPR Matched") var isBPRMatched: Boolean?,
    @ExcelColumn("Payment Type on platform") var paymentCodeAtPlatform: String?,
    @ExcelColumn("Payment Type on Sage") var paymentCodeAtSage: String?,
    @ExcelColumn("GL Code on platform") var bankCodeAtPlatform: Long?,
    @ExcelColumn("GL Code on Sage") var bankCodeAtSage: Long?,
    @ExcelColumn("GL Code Matched") var isGLCodeMatched: Boolean?,
    @ExcelColumn("Entity on platform") var entityCodeAtPlatform: Long?,
    @ExcelColumn("Entity on Sage") var entityCodeAtSage: Long?,
    @ExcelColumn("Entity Matched") var isEntityMatched: Boolean?,
    @ExcelColumn("Currency on platform") var currencyAtPlatform: String?,
    @ExcelColumn("Currency on Sage") var currencyAtSage: String?,
    @ExcelColumn("Currency Matched") var isCurrencyMatched: Boolean?,
    @ExcelColumn("Amount on platform") var amountAtPlatform: BigDecimal?,
    @ExcelColumn("Amount on Sage") var amountAtSage: BigDecimal?,
    @ExcelColumn("Amount Matched") var isAmountMatched: Boolean?,
    @ExcelColumn("UTR on platform") var utrAtPlatform: String?,
    @ExcelColumn("UTR on Sage") var utrAtSage: String?,
    @ExcelColumn("Utr Matched") var isUtrMatched: Boolean?,
    @ExcelColumn("Pan-Number on platform") var panNumberAtPlatform: String?,
    @ExcelColumn("Pan-Number on Sage") var panNumberAtSage: String?,
    @ExcelColumn("Pan Matched") var isPanMatched: Boolean?
)
