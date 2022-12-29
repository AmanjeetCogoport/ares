package com.cogoport.ares.model.settlement.event

data class EventData (
       var knockOffData : List<PaymentInfoRec>? = null,
       var errorMessage: String? =null
)
