package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SettlementRepository : CoroutineCrudRepository<Settlement, Long> {

    suspend fun findBySourceIdAndSourceTypeIn(sourceId: Long, sourceType: MutableList<SettlementType>): List<Settlement?>

    @Query(
        """
            select 
            id,
            source_id,
            source_type,
            destination_id,
            destination_type,
            currency,
            amount,
            led_currency,
            led_amount,
            sign_flag,
            settlement_date
            from settlements
            where source_id = :sourceId and source_type in (:sourceType)
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun findSettlement(sourceId: Long, sourceType: MutableList<SettlementType>, pageIndex: Int, pageSize: Int ): List<Settlement?>

    @Query(
        "SELECT count(1) FROM settlements where source_id = :sourceId"
    )
    suspend fun countSettlement(sourceId: Long, sourceType: List<SettlementType>): Long

    suspend fun countBySourceIdAndSourceTypeIn(sourceId: Long, sourceType: MutableList<SettlementType>): Long

    suspend fun findByDestinationIdAndDestinationType(destinationId: Long, destinationType: SettlementType, pageable: Pageable): List<Settlement?>
}
