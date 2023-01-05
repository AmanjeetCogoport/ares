package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Introspected
data class ParentJournalVoucherResponse(
    var id: String,
    var status: JVStatus?,
    var category: JVCategory?,
    var jvNum: String?,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now())
)
