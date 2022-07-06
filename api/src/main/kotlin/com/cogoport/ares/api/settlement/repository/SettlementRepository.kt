package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.settlement.SettledDocument
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

interface SettlementRepository : CoroutineCrudRepository<Settlement, Long> {


    suspend fun findBySourceIdAndSourceType( documentNumber: String, sourceType: AccountType,  pageable: Pageable ): List<SettledDocument?>

    suspend fun findByDestinationIdAndDestinationType( documentNumber: String, destinationType: AccountType, pageable: Pageable ): List<SettledDocument?>
}