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
    const val SERVICE_TYPE = "serviceType"
    const val INVOICE_CURRENCY = "invoiceCurrency"
    const val DASHBOARD_CURRENCY = "dashboardCurrency"
    const val COMPANY_TYPE = "companyType"
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
    const val CATEGORY = "category"
    const val TYPE = "type"
    const val FLAG = "flag"
    const val IS_SUSPENSE = "isSuspense"

    const val AR_ACCOUNT_CODE = 223000
    const val AP_ACCOUNT_CODE = 321000
    const val TDS_AR_ACCOUNT_CODE = 240000
    const val TDS_AP_ACCOUNT_CODE = 324001
    const val CSD_ACCOUNT_CODE = 214035

    val COGO_INDIA = listOf(101, 301)

    val ACC_MODE_PAYMENT_CODE_MAPPING = mapOf(
        "AP_PAY" to AP_ACCOUNT_CODE,
        "AR_REC" to AR_ACCOUNT_CODE,
        "AP_VTDS" to TDS_AP_ACCOUNT_CODE,
        "AR_CTDS" to TDS_AR_ACCOUNT_CODE,
        "CSD_REC" to CSD_ACCOUNT_CODE,
        "CSD_PAY" to CSD_ACCOUNT_CODE
    )
}
