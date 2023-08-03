package com.cogoport.ares.model.dunning.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class DunningCardData(
    var totalCustomers: Long?,
    var totalOutstandingAmount: BigDecimal?
) {
    @field:javax.persistence.Transient
    var activeCycles: Long? = null

    @field:javax.persistence.Transient
    var ledgerCurrency: String? = null
}
