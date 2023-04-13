package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.JournalCode

interface JournalCodeService {

    suspend fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode>
}
