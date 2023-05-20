package com.cogoport.ares.api.common.enums

enum class ThirdPartyApiNames(val value: String) {
    DELETE_JV_FROM_SAGE("DeleteJvFromSage")
}

enum class ThirdPartyApiType(val value: String) {
    JOURNAL_VOUCHERS("Journal Voucher")
}

enum class ThirdPartyObjectName(val value: String) {
    JOURNAL_VOUCHER("JOURNAL_VOUCHER")
}

enum class ThirdPartyResponseCode(val value: String) {
    SUCCESS("200"),
    FAILURE("500")
}
