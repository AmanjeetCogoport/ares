package com.cogoport.ares.api.common.enums

import com.cogoport.ares.model.payment.AccountType

enum class SignSuffix(val sign: Short, val accountType: AccountType) {
    REC(-1, AccountType.REC),
    PAY(1, AccountType.PAY),
    SALES_INVOICE(1, AccountType.SINV),
    PURCHASE_INVOICE(-1, AccountType.PINV),
    SALES_CREDIT_NOTE(-1, AccountType.SCN),
    SALES_REIMBURSEMENT_CREDIT_NOTE(-1, AccountType.SREIMBCN),
    PURCHASE_CREDIT_NOTE(1, AccountType.PCN),
    PURCHASE_DEBIT_NOTE(-1, AccountType.PDN),
    SALES_DEBIT_NOTE(1, AccountType.SDN),
    EXPENSE_INVOICE(-1, AccountType.EXP),
    PURCHASE_REMS_INVOICE(-1, AccountType.PREIMB),
    CTDS(-1, AccountType.CTDS),
    VTDS(1, AccountType.VTDS),
    SREIMB(1, AccountType.SREIMB),
    JVTDS(1, AccountType.JVTDS)
}
