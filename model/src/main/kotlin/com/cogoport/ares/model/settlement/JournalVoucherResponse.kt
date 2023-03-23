package com.cogoport.ares.model.settlement
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@JsonInclude
@Introspected
@MappedEntity
data class JournalVoucherResponse(
    var id: Long?,
    var entityCode: Int?,
    var entityId: UUID?,
    var jvNum: String?,
    var type: String?,
    var category: String?,
    var validityDate: Date?,
    var amount: BigDecimal?,
    var currency: String?,
    var status: JVStatus,
    var exchangeRate: BigDecimal?,
    var tradePartyId: UUID?,
    var tradePartyName: String?,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
    var description: String?,
    var accMode: AccMode?,
    var glCode: String?,
    var bankName: String?,
    var accountNumber: String?,
    var ledCurrency: String?,
    var parentJvId: String? = null,
    var sageUniqueId: String? = null,
    var migrated: Boolean? = false
)
