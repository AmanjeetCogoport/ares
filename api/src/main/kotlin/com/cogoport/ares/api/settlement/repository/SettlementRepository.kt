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
            where destination_id = :destId and destination_type::varchar = :destType
        """
    )
    suspend fun findByDestIdAndDestType(destId: Long, destType: SettlementType): List<Settlement?>

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
            au.document_value,
            s.destination_type,
            au.organization_id,
			au.acc_type::varchar,
            au.amount_curr - au.pay_curr as current_balance,
            au.currency,
            au.amount_curr as document_amount,
            s.amount as settled_amount,
            s.led_currency,
            s.led_amount,
            au.sign_flag,
            au.taxable_amount,
            0 as tds,
            au.transaction_date,
            au.amount_loc/au.amount_curr as exchange_rate,
            s.settlement_date,
            '' as status,
            coalesce(s1.amount, 0) as settled_tds
            FROM settlements s
            join account_utilizations au 
            LEFT JOIN settlements s1 on 
            s1.destination_id = au.document_no 
            AND s1.destination_type::VARCHAR = au.acc_type::VARCHAR
            AND s1.source_type IN ('CTDS','VTDS')
            AND s1.source_id = :sourceId
            ON s.destination_id = au.document_no
            AND s.destination_type::varchar = au.acc_type::varchar 
                WHERE au.amount_curr <> 0 
                AND s.source_id = :sourceId 
                AND s.source_type = :sourceType::SETTLEMENT_TYPE
                OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun findSettlement(sourceId: Long, sourceType: SettlementType, pageIndex: Int, pageSize: Int): List<SettledInvoice?>

    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE source_id = :sourceId 
            AND source_type = :sourceType::SETTLEMENT_TYPE
        """
    )
    suspend fun countSettlement(sourceId: Long, sourceType: SettlementType): Long

    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE destination_id = :destinationId 
            AND destination_type = :destinationType::SETTLEMENT_TYPE
            AND source_type = :sourceType::SETTLEMENT_TYPE
        """
    )
    suspend fun countDestinationBySourceType(destinationId: Long, destinationType: SettlementType, sourceType: SettlementType): Long
}
