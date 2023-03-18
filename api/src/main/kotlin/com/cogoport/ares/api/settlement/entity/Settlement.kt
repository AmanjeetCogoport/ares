package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@MappedEntity(value = "settlements")
data class Settlement(
    @field:Id @GeneratedValue
    var id: Long?,
    var sourceId: Long?,
    var sourceType: SettlementType?,
    var destinationId: Long,
    var destinationType: SettlementType,
    var currency: String?,
    var amount: BigDecimal?,
    var ledCurrency: String,
    var ledAmount: BigDecimal,
    var signFlag: Short,
    var settlementDate: Date,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
    var supportingDocUrl: String? = null,
    var isDraft: Boolean? = false,
    var settlementNum: String?
)
