package com.cogoport.ares.model.settlement.request

data class RejectSettleApproval(
    val incidentId: String,
    val incidentMappingId: String
)
