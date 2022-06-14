package com.cogoport.ares.api.utils

import java.sql.Timestamp
import java.time.LocalDate

fun Timestamp?.toLocalDate(): LocalDate? {
    return this?.toLocalDateTime()?.toLocalDate()
}
