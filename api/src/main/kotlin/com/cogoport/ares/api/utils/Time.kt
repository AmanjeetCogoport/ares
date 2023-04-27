package com.cogoport.ares.api.utils

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Time {
    companion object {
        private fun parseDate(dateStr: String, formatter: DateTimeFormatter): LocalDateTime {
            return LocalDateTime.parse(dateStr, formatter)
        }

        fun getBeginningOfFinancialYear(): LocalDateTime {
            val date: LocalDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val currentMonth: Int = date.monthValue
            var startYear: Int = date.year
            if (currentMonth < 4) startYear -= 1

            return parseDate(dateStr = "$startYear-04-01 00:00:00.000", formatter)
        }

        fun getEndOfFinancialYear(): LocalDateTime {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val endYear: Int = this.getBeginningOfFinancialYear().plusYears(1).minusDays(1).year

            return parseDate(dateStr = "$endYear-03-31 23:59:59.999", formatter)
        }
        fun getDateFromString(stringDate: String): String {
            try {
                val strDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(SimpleDateFormat("dd/MM/yyyy").parse(stringDate))
                return strDate
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace() // log write
                throw AresException(AresError.ERR_1533, "") // Invalid date format
            }
        }

        fun getTimeStampFromString(stringDate: String): Timestamp {
            try {
                val finalDate = SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(SimpleDateFormat("dd/MM/yyyy").parse(stringDate))

                return Timestamp.valueOf(finalDate)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace() // log write
                throw AresException(AresError.ERR_1534, "")
            }
        }
    }
}
