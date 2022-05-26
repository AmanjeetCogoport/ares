package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.http.annotation.QueryValue

data class DsoRequest(
    @QueryValue(AresConstants.ZONE) val zone: String?,
    @QueryValue(AresConstants.ROLE) val role: String?,
    @QueryValue(AresConstants.QUARTER) val quarter: ArrayList<Int>,
    @QueryValue(AresConstants.YEAR) val year: Int
)
