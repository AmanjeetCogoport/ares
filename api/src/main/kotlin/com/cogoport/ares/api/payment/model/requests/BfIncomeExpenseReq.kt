package com.cogoport.ares.api.payment.model.requests

import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfIncomeExpenseReq(
    var calenderYear: String? = null,
    var financeYearStart: String? = null,
    var financeYearEnd: String? = null,
    var serviceTypes: List<ServiceType>? = null,
    var isPostTax: Boolean? = true,
    var entityCode: MutableList<Int>? = mutableListOf(101, 301)
)
