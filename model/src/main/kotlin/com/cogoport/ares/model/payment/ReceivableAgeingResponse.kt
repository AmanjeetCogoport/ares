package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class ReceivableAgeingResponse(
    @JsonProperty("zone")
    var zone: List<String>,
    @JsonProperty("receivableByAgeViaZone")
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>
)
