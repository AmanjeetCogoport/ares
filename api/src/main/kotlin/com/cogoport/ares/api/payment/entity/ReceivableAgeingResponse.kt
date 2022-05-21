package com.cogoport.ares.api.payment.entity

data class ReceivableAgeingResponse(
    var zone: List<String>,
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>
)
