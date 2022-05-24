package com.cogoport.ares.model.payment

enum class PayMode(dbValue: String) {
    DD("dd"), CASH("cash"), CHEQUE("cheque"), NET_BANKING("net_banking"), UPI("upi"), BANK("bank")
}
