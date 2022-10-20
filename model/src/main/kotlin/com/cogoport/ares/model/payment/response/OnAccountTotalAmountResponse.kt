package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID
@MappedEntity
data class OnAccountTotalAmountResponse(
    var organizationId: UUID,
    var accMode: AccMode,
    var accType: AccountType,
    var paymentValue: BigDecimal
)
