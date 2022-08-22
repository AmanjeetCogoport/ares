package com.cogoport.ares.api.settlement.model

import com.cogoport.hades.model.incident.JournalVoucher
import io.micronaut.core.annotation.Introspected

@Introspected
data class JournalVoucherApproval(
    val incidentId: Long,
    val journalVoucherData: JournalVoucher
)
