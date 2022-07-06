package com.cogoport.ares.model.settlement

import java.sql.Timestamp

data class CheckRequest(
    val stackDetails: List<CheckDocument>,
    val settlementDate: Timestamp?
)
