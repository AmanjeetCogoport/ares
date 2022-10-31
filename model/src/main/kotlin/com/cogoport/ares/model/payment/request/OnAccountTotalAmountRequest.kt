package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import io.micronaut.core.annotation.Introspected
import java.util.UUID
@Introspected
data class OnAccountTotalAmountRequest(
    val accType: AccountType,
    val orgIdList: List<UUID>,
    val accMode: AccMode
)
