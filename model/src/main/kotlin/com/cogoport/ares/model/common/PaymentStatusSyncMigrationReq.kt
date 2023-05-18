package com.cogoport.ares.model.common

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class PaymentStatusSyncMigrationReq(
    @field: NotNull
    var documentNumbers: List<Long>?,
    @field: NotNull
    var accTypes: List<AccountType>?,
    @field: NotNull
    var accMode: AccMode?,
    @field: NotNull
    var performedBy: UUID?,
    @field: NotNull
    var performedByUserType: String?
)
