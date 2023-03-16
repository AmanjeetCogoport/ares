package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class ICJVRequest(
    var id: String?,
    val entityCode: Int,
    var entityId: UUID?,
    var jvNum: String?,
    val type: String,
    val tradePartyId: UUID?,
    val glCode: String,
    var tradePartyName: String?,
    var parentJvId: String?,
    var category: JVCategory?,
    var status: JVStatus?
)
