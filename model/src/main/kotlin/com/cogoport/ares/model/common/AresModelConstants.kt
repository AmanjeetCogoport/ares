package com.cogoport.ares.model.common

import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Calendar

object AresModelConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    val CURR_MONTH = LocalDate.now().monthValue
    const val ZONE = "zone"
    const val ROLE = "role"
    const val YEAR = "year"
    const val PAGE = "page"
    const val PAGE_LIMIT = "pageLimit"
    const val ORG_ID = "orgId"
    const val IMPORTER_EXPORTER_ID = "importerExporterId"
    const val SERVICE_PROVIDER_ID = "serviceProviderId"
    const val START_DATE = "startDate"
    const val END_DATE = "endDate"
    const val ENTITY_TYPE = "entityType"
    const val CURRENCY_TYPE = "currencyType"
    const val QUARTER_YEAR = "quarterYear"
    const val QUERY = "query"
    const val MONTH = "month"
    const val COUNT = "count"
    const val ACC_MODE = "accMode"
    const val STATUS = "status"
    const val ACCOUNT_TYPE = "accountType"
    const val SETTLEMENT_TYPE = "settlementType"
    const val DOCUMENT_NO = "documentNo"
    const val DOCUMENT_NOS = "documentNos"
    const val ENTITY_CODE = "EntityCode"

    const val AR_ACCOUNT_CODE = 223000
    const val AP_ACCOUNT_CODE = 321000
    const val TDS_AR_ACCOUNT_CODE = 240000
    const val TDS_AP_ACCOUNT_CODE = 324001
}
