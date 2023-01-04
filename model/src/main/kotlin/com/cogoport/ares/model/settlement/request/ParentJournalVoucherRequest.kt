package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Introspected
data class ParentJournalVoucherRequest(
    @field:Id @GeneratedValue var id: Long?,
    var status: JVStatus?,
    var category: JVCategory?,
    var jvNum: String?,
    var list: List<JournalVoucherRequest>,
    var createdBy: UUID?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now())
)
