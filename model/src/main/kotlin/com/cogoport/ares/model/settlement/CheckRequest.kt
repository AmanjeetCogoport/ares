package com.cogoport.ares.model.settlement

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class CheckRequest(
    val stackDetails: MutableList<CheckDocument>,
    val settlementDate: Timestamp = Timestamp.from(Instant.now()),
    val createdBy: UUID?
)
