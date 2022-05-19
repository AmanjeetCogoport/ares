package com.cogoport.ares.payment.model

data class ReceivableAgeingResponse(
    var zone: List<String>,
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>
)
