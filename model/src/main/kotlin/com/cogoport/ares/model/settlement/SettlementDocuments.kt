package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@Introspected
@MappedEntity
data class SettlementDocuments(
    var amount: BigDecimal?,
    var documentValue: String,
    var destinationId: Long,
    var destinationType: AccountType?,
    var sourceType: SettlementType?,
    var organizationId: UUID?,
    var orgSerialId: Long?,
    var flag: String?,

)
