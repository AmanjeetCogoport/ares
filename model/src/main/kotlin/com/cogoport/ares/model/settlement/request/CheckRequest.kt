package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.CheckDocument
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class CheckRequest(
    val stackDetails: MutableList<CheckDocument>,
    val settlementDate: Timestamp = Timestamp.from(Instant.now()),
    val createdBy: UUID?,
    val createdByUserType: String?
)
