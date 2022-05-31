package com.cogoport.ares.model.payment

enum class PayMode(dbValue: String) {
    DD("DD"), CASH("CASH"), CHEQUE("CHEQUE"), NET_BANKING("NET_BANKING"), UPI("UPI"), BANK("BANK")
}
