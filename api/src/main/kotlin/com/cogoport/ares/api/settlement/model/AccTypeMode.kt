package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType

data class AccTypeMode(
    val accMode: AccMode?,
    val accType: List<AccountType>,
)
