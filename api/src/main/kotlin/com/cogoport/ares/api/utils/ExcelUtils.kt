package com.cogoport.ares.api.utils

import com.cogoport.brahma.excel.ExcelSheetBuilder
import com.cogoport.brahma.excel.model.Color
import com.cogoport.brahma.excel.model.FontStyle
import com.cogoport.brahma.excel.model.Style
import java.io.File

object ExcelUtils {

    fun writeIntoExcel(dataList: List<Any>, excelName: String, sheetName: String): File {
        val file = ExcelSheetBuilder.Builder()
            .filename(excelName) // Filename without extension
            .sheetName(sheetName)
            .headerStyle( // Header style for all columns if you want to change the style of the individual header, you can pass style in the header object
                Style(
                    fontStyle = FontStyle.BOLD,
                    fontSize = 12,
                    fontColor = Color.BLACK,
                    background = Color.YELLOW
                )
            ).data(dataList).build()
        return file
    }
}
