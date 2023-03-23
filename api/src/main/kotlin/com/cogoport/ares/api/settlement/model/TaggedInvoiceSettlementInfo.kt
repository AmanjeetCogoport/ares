package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@MappedEntity
data class TaggedInvoiceSettlementInfo(
    val settlementId: Long,
    val sourceId: String,
    val sourceType: SettlementType,
    val destinationId: String,
    val destinationType: SettlementType,
    val currency: String,
    val amount: BigDecimal,
    val transRefNumber: String?,
    val settlementDate: Timestamp?,
    val taggedSettlementId: String?,
    val isVoid: Boolean
)
