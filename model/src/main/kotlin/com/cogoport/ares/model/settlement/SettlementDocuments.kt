package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@Introspected
@MappedEntity
data class SettlementDocuments(
    var documentValue: String?,
    var orgSerialId: Long?,
    var accountType: AccountType?,
    var paymentDocumentStatus: PaymentDocumentStatus?,
    var amount: BigDecimal?,
    var sageNumValue: String?,
    var organizationId: UUID?,
    var flag: String?,

)
