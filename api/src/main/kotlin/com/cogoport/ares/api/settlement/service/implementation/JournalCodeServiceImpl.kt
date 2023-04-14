package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.repository.JournalCodeRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalCodeService
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JournalCodeServiceImpl : JournalCodeService {

    @Inject
    lateinit var journalCodeRepository: JournalCodeRepository

    override suspend fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode> {
        val updatedPageLimit = pageLimit ?: 10
        return journalCodeRepository.getJournalCode(q, updatedPageLimit)
    }
}
