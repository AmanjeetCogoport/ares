package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue
@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountCollectionRequest(
    @QueryValue(AresModelConstants.START_DATE) val startDate: String? = null,
    @QueryValue(AresModelConstants.END_DATE) val endDate: String? = null,
    @QueryValue(AresModelConstants.ENTITY_TYPE) var entityType: Int? = null,
    @QueryValue(AresModelConstants.CURRENCY_TYPE) val currencyType: String? = null,
    @QueryValue(AresModelConstants.PAGE) val page: Int = 1,
    @QueryValue(AresModelConstants.PAGE_LIMIT) val pageLimit: Int = 10,
    @QueryValue(AresModelConstants.QUERY) val query: String? = null,
    @QueryValue(AresModelConstants.ACC_MODE) val accMode: AccMode?,
    @QueryValue("docType") val docType: String?,
    @QueryValue("paymentDocumentStatus") val paymentDocumentStatus: PaymentDocumentStatus?,
    @QueryValue("sortType") val sortType: String? = "createdAt",
    @QueryValue("sortBy") val sortBy: String? = "Desc"
)
