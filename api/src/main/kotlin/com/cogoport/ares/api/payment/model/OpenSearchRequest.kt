package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.AccMode
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.time.format.DateTimeFormatter
@Introspected
data class OpenSearchRequest(
    @QueryValue(AresConstants.ZONE) val zone: String? = null,
    @QueryValue(AresConstants.DATE) val date: String = AresConstants.CURR_DATE.toString().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    @QueryValue(AresConstants.QUARTER) val quarter: Int = AresConstants.CURR_QUARTER,
    @QueryValue(AresConstants.YEAR) val year: Int = AresConstants.CURR_YEAR,
    @QueryValue(AresConstants.ORG_ID) val orgId: String = "",
    @QueryValue(AresConstants.ORG_NAME) val orgName: String? = null,
    @QueryValue(AresConstants.ACC_MODE) val accMode: AccMode? = null,
)
