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
            s.settlement_date
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

    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE destination_id = :destinationId 
            AND destination_type = :destinationType
            AND source_type = :sourceType::SETTLEMENT_TYPE
        """
    )
    suspend fun countDestinationBySourceType(destinationId: Long, destinationType: SettlementType, sourceType: SettlementType): Long
}
