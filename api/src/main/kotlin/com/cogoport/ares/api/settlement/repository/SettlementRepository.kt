package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.model.Pageable
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository
interface SettlementRepository : CoroutineCrudRepository<Settlement, Long> {

    suspend fun findBySourceIdAndSourceType(sourceId: Long, sourceType: SettlementType, pageable: Pageable): List<Settlement?>

    suspend fun findByDestinationIdAndDestinationType(destinationId: Long, destinationType: SettlementType, pageable: Pageable): List<Settlement?>
}
