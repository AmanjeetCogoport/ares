package com.cogoport.ares.model.payment

enum class PayMode(dbValue: String) {
    DD("IMPS"), CASH("CASH"), CHEQUE("CHQ"), NET_BANKING("NEFT"), UPI("RTGS"), BANK("BANK")
}
