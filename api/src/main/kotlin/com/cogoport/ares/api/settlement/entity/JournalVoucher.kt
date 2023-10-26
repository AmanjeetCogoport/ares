package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@Introspected
@MappedEntity(value = "journal_vouchers")
data class JournalVoucher(
    @field:Id @GeneratedValue
    var id: Long?,
    val entityId: UUID?,
    val entityCode: Int?,
    var jvNum: String,
    var type: String?,
    var category: String,
    val validityDate: Date?,
    val amount: BigDecimal?,
    val ledAmount: BigDecimal?,
    val currency: String?,
    val ledCurrency: String,
    var status: JVStatus,
    val exchangeRate: BigDecimal?,
    var tradePartyId: UUID?,
    var tradePartyName: String?,
    var createdBy: UUID?,
    @DateCreated var createdAt: Timestamp?,
    @DateCreated var updatedAt: Timestamp?,
    var updatedBy: UUID?,
    var description: String?,
    var accMode: AccMode?,
    var glCode: String?,
    var parentJvId: Long?,
    var signFlag: Short?,
    var sageUniqueId: String? = null,
    var migrated: Boolean? = false,
    var deletedAt: Timestamp? = null,
    @MappedProperty(type = DataType.JSON)
    var additionalDetails: JVAdditionalDetails? = null
)
