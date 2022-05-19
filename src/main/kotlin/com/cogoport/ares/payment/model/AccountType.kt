package com.cogoport.ares.payment.model

enum class AccountType(dbValue: String) {
    SINV("sinv"), PINV("pinv"), SCN("scn"), SDN("sdn"), PCN("pcn"), PDN("pdn"), REC("rec"), PAY("pay");
}
