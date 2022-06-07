package com.cogoport.ares.model.payment

data class KnockOffUtilizationEvent(
    val knockOffUtilizationRequest: List<AccountPayablesFile>
)
