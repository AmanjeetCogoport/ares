package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.*

@Introspected
data class CollectionRequest(
    @QueryValue(AresConstants.ZONE) val zone: String?,
    @QueryValue(AresConstants.ROLE) val role: String?,
    @QueryValue(AresConstants.QUARTER) val quarter: Int = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR),
    @QueryValue(AresConstants.YEAR) val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)