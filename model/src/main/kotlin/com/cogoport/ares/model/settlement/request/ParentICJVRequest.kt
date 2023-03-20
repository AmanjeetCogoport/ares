package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.Date

@Introspected
data class ParentICJVRequest(
    @field:Id @GeneratedValue var id: Long?,
    var jvNum: String?,
    var accMode: AccMode?,
    var status: JVStatus?,
    var category: JVCategory,
    val validityDate: Date,
    val amount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    val exchangeRate: BigDecimal,
    var description: String?,
    var list: List<ICJVRequest>,
    var createdBy: UUID?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now())
)