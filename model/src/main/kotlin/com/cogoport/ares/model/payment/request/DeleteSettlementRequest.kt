package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

data class DeleteSettlementRequest(
    @QueryValue("documentNo") val documentNo: String,
    @QueryValue("settlementType") val settlementType: SettlementType,
    @QueryValue("deletedBy") val deletedBy: UUID?,
    @QueryValue("deletedByUserType") val deletedByUserType: String?
)
