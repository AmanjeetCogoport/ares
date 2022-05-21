package com.cogoport.ares.model.payment

enum class AccountType(dbValue: String) {
    SINV("sinv"), PINV("pinv"), SCN("scn"), SDN("sdn"), PCN("pcn"), PDN("pdn"), REC("rec"), PAY("pay");
}
