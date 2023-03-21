package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class CustomerOutstandingPaymentRequest(
    @field:NotNull(message = "orgId is mandate")
    val orgId: UUID? = null,
    val statusList: List<DocStatus>? = listOf(DocStatus.PARTIAL_UTILIZED, DocStatus.UNUTILIZED, DocStatus.UTILIZED),
    var entityCode: String? = "overall",
    val page: Int = 1,
    val pageLimit: Int = 10,
    val query: String? = "",
    var sortType: String? = "Desc",
    var sortBy: String? = "transactionDate",
    var accType: AccountType? = null
)
