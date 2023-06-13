package com.cogoport.ares.api.dunning.model.request

import com.cogoport.ares.api.dunning.model.DunningExceptionType
import io.micronaut.core.annotation.Introspected
import org.jetbrains.annotations.NotNull
import java.util.UUID

@Introspected
data class CreateDunningException(
    var exceptionFile: String? = null,
    var excludedRegistrationNos: MutableList<String>? = mutableListOf(),
    @NotNull var exceptionType: DunningExceptionType,
    @NotNull var createdBy: UUID,
    var cycleId: String? = null,
    var actionType: String?
)
