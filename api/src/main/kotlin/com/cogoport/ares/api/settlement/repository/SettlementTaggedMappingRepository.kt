package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.SettlementTaggedMapping
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SettlementTaggedMappingRepository : CoroutineCrudRepository<SettlementTaggedMapping, Long> {

    @NewSpan
    @Query(
        """
            WITH RECURSIVE bill_tree (settlement_id, utilized_settlement_id) AS (
            SELECT settlement_id, utilized_settlement_id
            FROM settlement_tagged_mappings
            WHERE settlement_id in (:settlementId) or utilized_settlement_id in (:settlementId)
            UNION ALL
            SELECT be.settlement_id, be.utilized_settlement_id
            FROM settlement_tagged_mappings be
            JOIN bill_tree bt ON be.settlement_id = bt.utilized_settlement_id
            ),
            bill (utilized_settlement_id) AS (SELECT distinct utilized_settlement_id FROM bill_tree)
            Select DISTINCT stm.utilized_settlement_id,settlement_id, stm.id, stm.created_at, stm.deleted_at from settlement_tagged_mappings stm join bill
             on stm.utilized_settlement_id = bill.utilized_settlement_id
        """
    )
    suspend fun getAllSettlementIds(settlementId: List<Long?>): List<SettlementTaggedMapping>
}
