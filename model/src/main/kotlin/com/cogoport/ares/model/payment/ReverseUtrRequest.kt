package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ReverseUtrRequest(
    val documentNo: Long,
    var updatedBy: UUID?,
    var performedByType: String?
)
