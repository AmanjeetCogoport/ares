package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.repository.JournalCodeRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalCodeService
import com.cogoport.ares.api.utils.Util
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JournalCodeServiceImpl : JournalCodeService {

    @Inject
    lateinit var journalCodeRepository: JournalCodeRepository

    @Inject
    lateinit var util: Util

    override suspend fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode> {
        val updatedPageLimit = pageLimit ?: 10
        val query = util.toQueryString(q)
        return journalCodeRepository.getJournalCode(query, updatedPageLimit)
    }
}
