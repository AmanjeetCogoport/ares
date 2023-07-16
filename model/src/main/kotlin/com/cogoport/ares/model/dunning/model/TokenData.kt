package com.cogoport.ares.model.dunning.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class TokenData(
    var dunningPaymentData: DunningPaymentData? = null,
    var dunningUserInviteData: DunningUserInviteData? = null
)
