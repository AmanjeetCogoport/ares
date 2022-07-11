package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SettlementRepository : CoroutineCrudRepository<Settlement, Long> {

    suspend fun deleteByIdIn(ids: List<Long>)
    @Query(
        """
            SELECT 
            s.id,
            s.source_id,
            s.source_type,
            s.destination_id,
            s.destination_type, 
            s.currency,
            s.amount,
            s.led_currency,
            s.led_amount,
            s.sign_flag,
            s.settlement_date,
            s.created_at,
            s.created_by,
            s.updated_at,
            s.updated_by
            FROM settlements s
            where source_id = :sourceId and source_type::varchar in (:sourceType)
        """
    )
    suspend fun findBySourceIdAndSourceType(sourceId: Long, sourceType: List<SettlementType>): List<Settlement?>

    @Query(
        """
        SELECT 
            s.id,
            s.destination_id,
            s.destination_type, 
            au.amount_curr - au.pay_curr as current_balance,
            s.currency,
            s.amount,
            s.led_currency,
            s.led_amount,
            s.sign_flag,
            s.settlement_date,
            '' as status
            FROM settlements s
            join account_utilizations au on s.destination_id = au.id
                WHERE s.source_id = :sourceId 
                AND s.source_type = :sourceType::SETTLEMENT_TYPE
                AND s.destination_type::varchar  NOT IN ('SECH', 'PECH', 'CTSD', 'VTDS') 
                OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun findSettlement(sourceId: Long, sourceType: SettlementType, pageIndex: Int, pageSize: Int): List<SettledInvoice?>

    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE source_id = :sourceId 
            AND source_type = :sourceType::SETTLEMENT_TYPE
            AND destination_type::varchar NOT IN ('SECH', 'PECH', 'CTSD', 'VTDS')
        """
    )
    suspend fun countSettlement(sourceId: Long, sourceType: SettlementType): Long
}
