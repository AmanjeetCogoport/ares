package com.cogoport.ares.model.payment

data class ReceivableAgeingResponse(
    var zone: List<String>,
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>
)
