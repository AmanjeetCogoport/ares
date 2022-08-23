package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AccountType

data class DocIdAccTypePair(
        var id: String,
        var accType: AccountType
)