package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected
import org.jetbrains.annotations.NotNull
import java.util.UUID

@Introspected
data class TradePartyOutstandingReq(
    @field: NotNull
    var orgIds: List<UUID>? = listOf()
)
