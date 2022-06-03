package com.cogoport.ares.model.common

import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Calendar

object AresModelConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    const val ZONE = "zone"
    const val ROLE = "role"
    const val QUARTER = "quarter"
    const val YEAR = "year"
    const val ORG_NAME = "orgName"
    const val PAGE = "page"
    const val PAGE_LIMIT = "pageLimit"
    const val ORG_ID = "orgId"

    const val AR_ACCOUNT_CODE =223000
    const val AP_ACCOUNT_CODE = 321000
    const val TDS_AR_ACCOUNT_CODE=240000
    const val TDS_AP_ACCOUNT_CODE = 324001
}
