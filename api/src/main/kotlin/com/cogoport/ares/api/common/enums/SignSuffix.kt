package com.cogoport.ares.api.common.enums

import com.cogoport.ares.model.payment.AccountType

enum class SignSuffix(val sign: Short, val accountType: AccountType) {
    REC(-1, AccountType.REC),
    PAY(1, AccountType.PAY),
    SALES_INVOICE(1, AccountType.SINV),
    PURCHASE_INVOICE(-1, AccountType.PINV),
    SALES_CREDIT_NOTE(-1, AccountType.SCN),
    PURCHASE_CREDIT_NOTE(1, AccountType.PCN),
    PURCHASE_DEBIT_NOTE(-1, AccountType.PDN),
    SALES_DEBIT_NOTE(1, AccountType.SDN),
    PURCHASE_REMS_INVOICE(-1, AccountType.PREIMB),
    CTDSP(-1, AccountType.CTDSP),
    CTDS(-1, AccountType.CTDS),
    VTDS(1, AccountType.VTDS),
}
