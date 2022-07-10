package com.cogoport.ares.model.settlement

import java.sql.Timestamp

data class CheckRequest(
    val stackDetails: MutableList<CheckDocument>,
    val settlementDate: Timestamp?,
    val type: String
)
