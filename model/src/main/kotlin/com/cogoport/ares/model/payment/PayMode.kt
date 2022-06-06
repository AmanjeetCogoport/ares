package com.cogoport.ares.model.payment

enum class PayMode(dbValue: String) {
    IMPS("IMPS"), CASH("CASH"), CHQ("CHQ"), NEFT("NEFT"), RTGS("RTGS"), BANK("BANK")
}
