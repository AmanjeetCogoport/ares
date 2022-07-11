package com.cogoport.ares.model.settlement

import java.sql.Timestamp
import java.time.Instant

data class CheckRequest(
    val stackDetails: MutableList<CheckDocument>,
    val settlementDate: Timestamp = Timestamp.from(Instant.now())
)
