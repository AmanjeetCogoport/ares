package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.hades.model.incident.JournalVoucher
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

data class JournalVoucherApproval(
    val incidentId: Long,
    val journalVoucherData: JournalVoucher
)
