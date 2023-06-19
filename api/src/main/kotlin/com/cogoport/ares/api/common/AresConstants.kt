package com.cogoport.ares.api.common

import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.ServiceType
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.IsoFields

object AresConstants {
    val ARES_USER_ID = "d815466f-f39f-414e-a799-5cd178da57ce"

    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = LocalDate.now().year
    val CURR_DATE: Timestamp = Timestamp.from(Instant.now())
    val CURR_MONTH = LocalDate.now().monthValue

    // Open search indexes
    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val INVOICE_OUTSTANDING_INDEX = "index_ares_invoice_outstanding"
    const val ACCOUNT_UTILIZATION_INDEX = "index_account_utilization"
    val COGO_ENTITIES = listOf(101, 201, 301, 401, 501)
    const val SUPPLIERS_OUTSTANDING_OVERALL_INDEX = "supplier_outstanding_overall"
    const val CUSTOMERS_OUTSTANDING_OVERALL_INDEX = "customer_outstanding_overall"
    const val PAYABLES_STATS_INDEX = "payables_stats"
    const val KEY_DELIMITER = "_"

    const val ROLE_ZONE_HEAD = "b2af88f9-84e4-44fd-92f8-12f74c55e5ae"
    const val ROLE_SUPER_ADMIN = "SuperAdmin"

    const val ZONE = "zone"
    const val QUARTER = "quarter"
    const val YEAR = "year"
    const val DATE = "date"
    const val ORG_ID = "orgId"
    const val ORG_NAME = "orgName"
    const val PAGE = "page"
    const val PAGE_LIMIT = "pageLimit"
    const val ACC_MODE = "AccMode"
    const val SERVICE_TYPE = "serviceType"
    const val INVOICE_CURRENCY = "invoiceCurrency"
    const val DASHBOARD_CURRENCY = "dashboardCurrency"
    const val COGO_ENTITY_ID = "cogoEntityId"

    const val YEAR_DATE_FORMAT = "yyyy-MM-dd"

    const val ACCMODE = "accMode"
    const val MODE = "AR"
    const val ORGANIZATION_ID = "organizationId"
    const val TRANSACTION_DATE = "transactionDate"

    const val DEFAULT_TDS_RATE = 2
    const val DECIMAL_NUMBER_UPTO = 12
    const val ROUND_DECIMAL_TO = 4
    const val NO_DEDUCTION_RATE = 0
    const val ROUND_OFF_DECIMAL_TO_2 = 2

    const val PAYING_PARTY = "self,paying_party"

    const val PAYMENTS = "payments"
    const val SUSPENSE_ACCOUNT = "suspense_account"
    const val ACCOUNT_UTILIZATIONS = "account_utilizations"
    const val SETTLEMENT = "settlement"
    const val INCIDENT_MAPPINGS = "incident_mappings"
    const val JOURNAL_VOUCHERS = "journal_vouchers"
    const val PARENT_JOURNAL_VOUCHERS = "parent_journal_vouchers"

    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
    const val VOID = "VOID"
    const val ALL = "All"

    // Settlement Constants
    const val PAYMENT = "PAYMENT"
    const val INVOICE = "INVOICE"
    const val CREDIT_NOTE = "CREDIT_NOTE"
    const val JV = "JV"
    const val TDS = "TDS"
    const val IND = "IND"
    const val VND = "VND"

    const val UNIFIED = "dms"

    const val LIMIT = 10000

    var WHITE_LISTED_PATHS = mutableListOf(
        "get_payments_health",
        "get_payments_service_discovery_reachability",
        "get_payments_service_discovery_plutus_reachability",
        "get_payments_service_discovery_hades_reachability",
        "get_payments_service_discovery_kuber_reachability",
        "get_payments_download"
    )

    val ENTITY_ID = mapOf(
        101 to "6fd98605-9d5d-479d-9fac-cf905d292b88",
        201 to "c7e1390d-ec41-477f-964b-55423ee84700",
        301 to "ee09645b-5f34-4d2e-8ec7-6ac83a7946e1",
        401 to "04bd1037-c110-4aad-8ecc-fc43e9d4069d",
        501 to "b67d40b1-616c-4471-b77b-de52b4c9f2ff"
    )
    val TAGGED_ENTITY_ID_MAPPINGS = mapOf(
        "6fd98605-9d5d-479d-9fac-cf905d292b88" to 101,
        "c7e1390d-ec41-477f-964b-55423ee84700" to 201,
        "ee09645b-5f34-4d2e-8ec7-6ac83a7946e1" to 301,
        "04bd1037-c110-4aad-8ecc-fc43e9d4069d" to 401,
        "b67d40b1-616c-4471-b77b-de52b4c9f2ff" to 501,
    )

    val LEDGER_CURRENCY = mapOf(
        101 to "INR",
        201 to "EUR",
        301 to "INR",
        401 to "SGD",
        501 to "VND",
    )

    val OCEAN_SERVICES = listOf(
        ServiceType.FCL_FREIGHT,
        ServiceType.LCL_FREIGHT,
        ServiceType.FCL_CUSTOMS,
        ServiceType.LCL_CUSTOMS,
        ServiceType.FCL_CUSTOMS_FREIGHT,
        ServiceType.LCL_CUSTOMS_FREIGHT,
        ServiceType.FCL_CFS,
        ServiceType.FCL_FREIGHT_LOCAL
    )
    val AIR_SERVICES = listOf(
        ServiceType.AIR_CUSTOMS,
        ServiceType.AIR_FREIGHT,
        ServiceType.AIR_CUSTOMS_FREIGHT,
        ServiceType.AIR_FREIGHT_LOCAL,
        ServiceType.DOMESTIC_AIR_FREIGHT
    )
    val SURFACE_SERVICES = listOf(
        ServiceType.FTL_FREIGHT,
        ServiceType.LTL_FREIGHT,
        ServiceType.HAULAGE_FREIGHT,
        ServiceType.TRAILER_FREIGHT,
        ServiceType.RAIL_DOMESTIC_FREIGHT,
        ServiceType.TRUCKING
    )

    val CC_MAIL_FOR_SETTLEMENTS_MATCHING_FAILED_ON_SAGE = listOf(
        "preeti.pandey@cogoport.com",
        "rajnish.bahl@cogoport.com",
        "pankaj.pal@cogoport.com",
        "abhishek.kumar@cogoport.com"
    )

    const val FAILED_SETTLEMENTS_MATCHING_ON_SAGE_TEMPLATE = "Failed Settlements Matching on Sage"
    const val NO_REPLY = "no-reply@cogoport.com"
    const val RECIPIENT_EMAIL_FOR_EVERYDAY_AUTO_GENERATION_SETTLEMENTS_MATCHING_FAILED_EMAIL = "sachin.yadav@cogoport.com"

    val CREDIT_DAYS_MAPPING = mapOf(
        30L to Pair(0L, 30L),
        60L to Pair(30L, 60L),
        90L to Pair(60L, 90L)
    )
    val ON_ACCOUNT_ACCOUNT_TYPE = listOf<AccountType>(
        AccountType.REC,
        AccountType.CTDS
    )

    val OUTSTANDING_ACCOUNT_TYPE = listOf<AccountType>(
        AccountType.SINV,
        AccountType.SCN,
        AccountType.SREIMB,
        AccountType.SREIMBCN
    )
    var DUNNING_NEW_INVOICE_GENERATION_TEMPLATE = "dunning_cycle_invoice_generated_mail"
    var DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE = "balance_confirmation_email"
    var DUNNING__SOA_MAIL_TEMPLATE = "dunning_cycle_soa_mail"

    var EXCLUDED_CREDIT_CONTROLLERS = mutableListOf("d6838384-b53c-48a1-9c0f-3aca54b53fa9", "d80c5421-7bd8-4ec5-88fa-9a02afb936eb", "59559d86-853d-41b5-a613-a1fd7b3eb76e")

    var DUNNING_WORK_SCOPES = mutableListOf("i_am_finance_head", "i_am_logistics_manager", "i_work_in_finance", "i_am_operation_manager", "i_work_in_procurement", "other")

    var DUNNING_EXCLUDE_WORK_SCOPES = mutableListOf("i_am_owner", "i_am_managing_director", "i_am_ceo", "i_am_president", "i_am_partner")

    var COLLECTION_ACCOUNT_EMAIL = "collection@cogoport.com"

    var COLLECTION_ACCOUNT_NAME = "Cogoport Collection Team"

    var DUNNING_VALID_TEMPLATE_NAMES = mutableListOf(
        "dunning_cycle_invoice_generated_mail", "balance_confirmation_email", "dunning_cycle_soa_mail"
    )

    var DUNNING_BANK_DETAILS = mapOf(
        101 to "<div><div style=\"border:1px solid #000;height:250px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For INR Payments: COGO Freight PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p><p>INR Account:  409000876343</p></div></div></div><div style=\"border:1px solid #000;height:250px; margin: 0px 8px 0px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000;\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: COGO Freight PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p><p>EEFC Account (USD)- 409000824933</p></div></div>",
        201 to "<div><div style=\"border:1px solid #000;height:300px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: COGOPORT PRIVATE LIMITED</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Ing Bank N V</p><p>Account Number: NL 92 INGB 0020 1127 69</p><p>SWIFT Code: INGBNL2A</p><p>IFSC/Bank Code: INGBN</p><p>Branch Code: L2A</p></div></div></div><div style=\"border:1px solid #000;height:300px; margin: 0px 8px 0px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000;\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For EUR Payments: COGOPORT PRIVATE LIMITED</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Ing Bank N V</p><p>Account Number: NL 18 INGB 0670 3440 95</p><p>SWIFT Code: INGBNL2A</p><p>IFSC /Bank Code: INGBN</p><p>Branch Code: L2A</p></div></div>",
        301 to "<div><div style=\"border:1px solid #000;height:250px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For INR Payments: COGOPORT PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p><p>INR Account:  409001406475</p></div></div></div><div style=\"border:1px solid #000;height:250px; margin: 0px 8px 0px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000;\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: COGOPORT PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p><p>EEFC Account(USD)- 409001685863</p></div></div>",
        401 to "<div><div style=\"border:1px solid #000;height:300px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For SGD Payments: COGOPORT PRIVATE LIMITED</p></div><div style=\"text-align:center;line-height:21px;\"><p>Name of Bank: Citibank N.A.</p><p>Account Number: 0-021112-003</p><p>SWIFT Code: CITISGSG</p><p>IFSC /Bank Code: 7214</p><p>Branch Code: 001</p></div></div></div>"
    )

    enum class TimeZone {
        GMT,
        IST,
        VNM,
        UTC
    }

    var TIME_ZONE_DIFFENRENCE_FROM_GMT = mapOf<TimeZone, Long>(
        TimeZone.GMT to 0.toLong(),
        TimeZone.IST to 19800000.toLong(),
        TimeZone.UTC to 28800000.toLong(),
        TimeZone.VNM to 25200000.toLong()
    )

    const val MAX_DAY_IN_MONTH_FOR_DUNNING: Int = 28

    const val ARES_EXCHANGE = "ares"

    val QUEUES = listOf(
        "update-supplier-details",
        "knockoff-payables",
        "reverse-utr",
        "unfreeze-credit-consumption",
        "receivables-outstanding-data",
        "update-utilization-amount",
        "create-account-utilization",
        "update-account-utilization",
        "delete-account-utilization",
        "update-account-status",
        "settlement-migration",
        "sage-payment-migration",
        "sage-jv-migration",
        "send-payment-details-for-autoKnockOff",
        "update-customer-details",
        "migrate-settlement-number",
        "update-settlement-bill-updated",
        "tagged-bill-auto-knockoff",
        "delete-invoices-not-present-in-plutus",
        "migrate-gl-codes",
        "post-jv-to-sage",
        "migrate-new-period",
        "migrate-jv-pay-loc",
        "send-payment-details",
        "post-payment-to-sage",
        "sage-payment-num-migration",
        "bulk-post-payment-to-sage",
        "bulk-post-settlement-to-sage",
        "partial-payment-mismatch",
        "bulk-update-payment-and-post-on-sage",
        "bulk-post-payment-from-sage",
        "send-email"
    )

    val RETRY_QUEUES = listOf(
        "update-supplier-details",
        "knockoff-payables",
        "reverse-utr",
        "unfreeze-credit-consumption",
        "receivables-outstanding-data",
        "update-utilization-amount",
        "create-account-utilization",
        "update-account-utilization",
        "delete-account-utilization",
        "update-account-status",
        "settlement-migration",
        "sage-payment-migration",
        "sage-jv-migration",
        "send-payment-details-for-autoKnockOff",
        "update-customer-details",
        "migrate-settlement-number",
        "update-settlement-bill-updated",
        "tagged-bill-auto-knockoff",
        "delete-invoices-not-present-in-plutus",
        "migrate-gl-codes",
        "post-jv-to-sage",
        "migrate-new-period",
        "migrate-jv-pay-loc",
        "send-payment-details",
        "post-payment-to-sage",
        "sage-payment-num-migration",
        "bulk-post-payment-to-sage",
        "bulk-post-settlement-to-sage",
        "partial-payment-mismatch",
        "bulk-update-payment-and-post-on-sage",
        "bulk-post-payment-from-sage"
    )

    const val performedByUserNameForMail = "Business Finance Tech Team"
    const val SAGE_PLATFORM_REPORT = "sage_platform_report"
    const val RECIPIENT_EMAIL_FOR_EVERYDAY_SAGE_PLATFORM_REPORT = "bhanugoban@cogoport.com"

    val CC_MAIL_ID_FOR_EVERYDAY_SAGE_PLATFORM_REPORT = mutableListOf<String>(
        "abhishek.kumar@cogoport.com",
        "vivek.garg@cogoport.com",
        "shikhar.tyagi@cogoport.com",
        "suhas.latelwar@cogoport.com"
    )

    const val BUSINESS_FINANCE_TECH_TEAM = "649f3792-b28f-4862-9683-5d5af2c574b4"
}
