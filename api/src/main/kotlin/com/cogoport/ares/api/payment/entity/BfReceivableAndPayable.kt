package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.api.payment.model.response.OnAccountAndOutstandingResp
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import javax.persistence.Transient

@Introspected
@MappedEntity
data class BfReceivableAndPayable(
    var nonOverdueAmount: BigDecimal? = 0.toBigDecimal(),
    var overdueAmount: BigDecimal? = 0.toBigDecimal(),
    var notPaidDocumentCount: Long? = 0,
    var thirtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var sixtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var ninetyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var oneEightyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var threeSixtyDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var threeSixtyPlusDayOverdue: BigDecimal? = 0.toBigDecimal(),
    var tillYesterdayTotalOutstanding: BigDecimal? = 0.toBigDecimal()
) {
    @field:Transient
    var onAccountAndOutStandingData: MutableList<OnAccountAndOutstandingResp>? = null

    @field:Transient
    var onAccountChangeFromYesterday: BigDecimal? = 0.toBigDecimal()

    @field:Transient
    var outstandingChangeFromYesterday: BigDecimal? = 0.toBigDecimal()

    @field:Transient
    var currency: String? = "INR"
}
