package com.cogoport.ares.api.dunning

import com.cogoport.ares.api.dunning.model.SeverityEnum

object DunningConstants {

    val CREDIT_DAYS_MAPPING = mapOf(
        30L to Pair(0L, 30L),
        60L to Pair(31L, 60L),
        90L to Pair(61L, 90L)
    )
    var DUNNING_NEW_INVOICE_GENERATION_TEMPLATE = "dunning_cycle_invoice_generated_mail"
    var DUNNING_BALANCE_CONFIRMATION_MAIL_TEMPLATE = "balance_confirmation_email"
    var DUNNING_SOA_MAIL_TEMPLATE = "dunning_cycle_soa_mail"

    var EXCLUDED_CREDIT_CONTROLLERS = mutableListOf("d6838384-b53c-48a1-9c0f-3aca54b53fa9", "d80c5421-7bd8-4ec5-88fa-9a02afb936eb", "59559d86-853d-41b5-a613-a1fd7b3eb76e")

    var DUNNING_WORK_SCOPES = mutableListOf("i_am_finance_head", "i_am_logistics_manager", "i_work_in_finance", "i_am_operation_manager", "i_work_in_procurement", "other")

    var DUNNING_EXCLUDE_WORK_SCOPES = mutableListOf("i_am_owner", "i_am_managing_director", "i_am_ceo", "i_am_president", "i_am_partner")

    var COLLECTION_ACCOUNT_EMAIL = "collection@cogoport.com"

    var COLLECTION_ACCOUNT_NAME = "Cogoport Collection Team"

    var DUNNING_VALID_TEMPLATE_NAMES = mutableListOf(
        "dunning_cycle_invoice_generated_mail", "balance_confirmation_email", "dunning_cycle_soa_mail"
    )

    var DUNNING_BANK_DETAILS = mapOf(
        101 to "<div><div style=\"border:1px solid #000;height:250px;margin: 0px 8px 16px 8px;\">" +
            "<div style=\"height:65px;border-bottom:2px solid #000\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">" +
            "For INR Payments: COGO Freight PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p>" +
            "<p>IFSC Code: RATN0000088</p><p>INR Account:  409000876343</p></div></div></div><div style=\"border:1px solid #000;height:250px; margin: 0px 8px 0px 8px;\">" +
            "<div style=\"height:65px;border-bottom:2px solid #000;\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">" +
            "For USD Payments: COGO Freight PVT LTD</p></div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p>" +
            "<p>IFSC Code: RATN0000088</p><p>EEFC Account (USD)- 409000824933</p></div></div>",
        201 to "<div><div style=\"border:1px solid #000;height:300px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: COGOPORT PRIVATE LIMITED</p></div>" +
            "<div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Ing Bank N V</p><p>Account Number: NL 92 INGB 0020 1127 69</p><p>SWIFT Code: INGBNL2A</p>" +
            "<p>IFSC/Bank Code: INGBN</p><p>Branch Code: L2A</p></div></div></div><div style=\"border:1px solid #000;height:300px; margin: 0px 8px 0px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000;\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For EUR Payments: COGOPORT PRIVATE LIMITED</p></div>" +
            "<div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Ing Bank N V</p><p>Account Number: NL 18 INGB 0670 3440 95</p><p>SWIFT Code: INGBNL2A</p>" +
            "<p>IFSC /Bank Code: INGBN</p><p>Branch Code: L2A</p></div></div>",
        301 to "<div><div style=\"border:1px solid #000;height:250px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For INR Payments: COGOPORT PVT LTD</p>" +
            "</div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p>" +
            "<p>INR Account:  409001406475</p></div></div></div><div style=\"border:1px solid #000;height:250px; margin: 0px 8px 0px 8px;\">" +
            "<div style=\"height:65px;border-bottom:2px solid #000;\"><p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: COGOPORT PVT LTD</p>" +
            "</div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: RBL</p><p>Branch: Lower Parel</p><p>IFSC Code: RATN0000088</p><p>EEFC Account(USD)- 409001685863</p></div></div>",
        401 to "<div><div style=\"border:1px solid #000;height:300px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For SGD Payments: COGOPORT PRIVATE LIMITED</p>" +
            "</div><div style=\"text-align:center;line-height:21px;\"><p>Name of Bank: Citibank N.A.</p><p>Account Number: 0-021112-003</p><p>SWIFT Code: CITISGSG</p>" +
            "<p>IFSC /Bank Code: 7214</p><p>Branch Code: 001</p></div></div></div>",
        501 to "<div><div style=\"border:1px solid #000;height:250px;margin: 0px 8px 16px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For VND Payments: CÔNG TY TNHH COGOPORT VIETNAM</p>" +
            "</div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Citibank, N.A, Ha Noi Branc</p><p>SWIFT Code: CITIVNVX</p><p>BANK CODE: 0202955002</p>" +
            "</div></div></div><div style=\"border:1px solid #000;height:250px; margin: 0px 8px 0px 8px;\"><div style=\"height:65px;border-bottom:2px solid #000;\">" +
            "<p style=\"text-align:center;font-weight:600;font-size:15px;line-height:25px;padding:5px\">For USD Payments: CÔNG TY TNHH COGOPORT VIETNAM</p>" +
            "</div><div style=\"text-align:center;line-height:20px;\"><p>Name of Bank: Citibank, N.A, Ha Noi Branch</p><p>SWIFT Code: CITIVNVX</p><p>BANK CODE: 0202955037</p></div></div>\n"
    )

    var SEGMENT_MAPPING = mapOf(
        "sme" to "MID_SIZE",
        "enterprise" to "ENTERPRISE",
        "large" to "LONG_TAIL"
    )

    enum class TimeZone {
        GMT,
        IST,
        VNM,
        UTC
    }

    var TIME_ZONE_DIFFERENCE_FROM_GMT = mapOf(
        TimeZone.GMT to 0.toLong(),
        TimeZone.IST to 19800000.toLong(),
        TimeZone.UTC to 28800000.toLong(),
        TimeZone.VNM to 25200000.toLong()
    )

    val DUNNING_SEVERITY_LEVEL = mapOf(
        SeverityEnum.LOW to 1,
        SeverityEnum.MEDIUM to 2,
        SeverityEnum.HIGH to 3
    )

    const val EXTRA_TIME_TO_PROCESS_DATA_DUNNING = 3600

    const val MAX_DAY_IN_MONTH_FOR_DUNNING: Int = 28

    const val MAX_PAGE_SIZE = 10000
}
