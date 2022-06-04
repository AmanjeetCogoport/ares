package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountCollectionResponse(
    val payments: List<Payment?>,
    val totalRecords: Int,
    val totalPage: Int,
    val page: Int
)
