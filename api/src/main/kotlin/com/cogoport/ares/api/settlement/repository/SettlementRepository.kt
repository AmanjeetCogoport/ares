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

    suspend fun findByIdIn(ids: List<Long>): List<Settlement>

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
         WITH INVOICES AS (
            SELECT 
                au.id,
                s.source_id AS payment_document_no,
                s.destination_id,
                au.document_value,
                s.destination_type,
                au.organization_id,
                au.acc_type::varchar,
                COALESCE(au.amount_curr - au.pay_curr,0) AS current_balance,
                au.currency AS currency,
                s.currency AS payment_currency,
                COALESCE(au.amount_curr,0) AS document_amount,
                sum(COALESCE(s.amount,0)) AS settled_amount,
                s.led_currency,
                sum(COALESCE(s.led_amount,0)) AS led_amount,
                au.sign_flag,
                COALESCE(au.taxable_amount,0) AS taxable_amount,
                sum(COALESCE(s.amount, 0)) AS tds,
                au.transaction_date,
                au.amount_loc/au.amount_curr AS exchange_rate,
                au.acc_mode
            FROM settlements s
            JOIN account_utilizations au ON
                s.destination_id = au.document_no
                AND s.destination_type::VARCHAR = au.acc_type::VARCHAR
            WHERE au.amount_curr <> 0 
                AND s.source_id = :sourceId
                AND s.source_type = :sourceType::SETTLEMENT_TYPE
            GROUP BY au.id, s.source_id, s.destination_id, s.destination_type, s.currency, s.led_currency
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        ),
        TAX AS (
            SELECT s.destination_id, s.currency, s.source_id as tds_document_no, s.source_type as tds_type,
                sum(CASE WHEN s.source_id = :sourceId AND s.source_type IN ('CTDS','VTDS') THEN s.amount ELSE 0 END) AS tds,
                sum(CASE WHEN s.source_type IN ('NOSTRO') THEN s.amount ELSE 0 END) AS nostro_amount,
                sum(CASE WHEN s.source_type IN ('CTDS','VTDS') THEN s.amount ELSE 0 END) AS settled_tds
            FROM settlements s
            WHERE s.destination_id in (SELECT DISTINCT destination_id FROM INVOICES) 
                AND s.destination_type in (SELECT DISTINCT destination_type from INVOICES)
                AND s.source_type IN ('NOSTRO','VTDS','CTDS')
            GROUP BY s.destination_id, s.currency, s.source_id, s.source_type
        )
        SELECT I.id, I.payment_document_no, I.destination_id, I.document_value, I.destination_type, I.organization_id,
            I.acc_type, I.current_balance, I.currency, I.payment_currency, I.document_amount, I.settled_amount, 
            I.led_currency, I.led_amount, I.sign_flag, I.taxable_amount, I.transaction_date, I.exchange_rate,
            T.tds_document_no, T.tds_type, COALESCE(T.tds,0) as tds, COALESCE(T.nostro_amount,0) as nostro_amount, 
            COALESCE(T.settled_tds,0) as settled_tds, T.currency AS tds_currency, I.acc_mode
        FROM INVOICES I
        LEFT JOIN TAX T ON T.destination_id = I.destination_id
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
