package com.cogoport.ares.model.settlement
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@JsonInclude
@Introspected
data class JournalVoucherResponse(
    var id: Long?,
    val entityCode: Int?,
    val entityId: UUID?,
    val jvNum: String?,
    val type: String?,
    val category: String?,
    val validityDate: Date?,
    val amount: BigDecimal?,
    val currency: String?,
    val status: JVStatus,
    val exchangeRate: BigDecimal?,
    val tradePartyId: UUID?,
    val tradePartyName: String?,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
    var description: String?,
    var accMode: AccMode?,
    var parentJvId: String? = null,
)
