package com.cogoport.ares.api.migration.model

import com.cogoport.ares.api.migration.constants.MigrationRecordType
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class PayLocUpdateRequest(
    val sageOrganizationId: String?,
    var documentValue: String?,
    val amtLoc: BigDecimal?,
    val payCurr: BigDecimal?,
    val payLoc: BigDecimal?,
    val accMode: String?,
    val recordType: MigrationRecordType
)
