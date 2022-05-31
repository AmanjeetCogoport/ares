package com.cogoport.ares.api.utils

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import java.sql.Timestamp

class Utilities {
    companion object DateUtils {
        fun getTimeStampFromString(stringDate: String): Timestamp {

            try {
                val year = stringDate.substring(0, 4)
                val month = stringDate.substring(5, 7)
                val day = stringDate.substring(8, 10)

                val finalDate = "$year-$month-$day 00:00:00"
                return Timestamp.valueOf(finalDate)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace() // log write
                throw AresException(AresError.ERR_1003, "date") // Invalid date format
            }
        }
    }
}
