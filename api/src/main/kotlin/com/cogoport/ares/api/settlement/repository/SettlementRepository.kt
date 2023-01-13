package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.model.PaymentInfo
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.event.PaymentInfoRec
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.sql.Timestamp

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SettlementRepository : CoroutineCrudRepository<Settlement, Long> {

    @NewSpan
    suspend fun deleteByIdIn(ids: List<Long>)

    @NewSpan
    suspend fun findByIdIn(ids: List<Long>): List<Settlement>

    @NewSpan
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
            s.updated_by,
            s.supporting_doc_url
            FROM settlements s
            where destination_id = :destId and deleted_at is null and destination_type::varchar = :destType
        """
    )
    suspend fun findByDestIdAndDestType(destId: Long, destType: SettlementType): List<Settlement?>

    @NewSpan
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
            s.updated_by,
            s.supporting_doc_url
            FROM settlements s
            where source_id = :sourceId and deleted_at is null and source_type::varchar in (:sourceType)
        """
    )
    suspend fun findBySourceIdAndSourceType(sourceId: Long, sourceType: List<SettlementType>): List<Settlement?>

    @NewSpan
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
                AND s.deleted_at is null
                AND au.deleted_at is null
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
                AND s.deleted_at is null
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

    @NewSpan
    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE source_id = :sourceId 
            AND source_type = :sourceType::SETTLEMENT_TYPE
            AND deleted_at is null
        """
    )
    suspend fun countSettlement(sourceId: Long, sourceType: SettlementType): Long

    @NewSpan
    @Query(
        """SELECT count(1) 
            FROM settlements 
            WHERE destination_id = :destinationId 
            AND destination_type = :destinationType::SETTLEMENT_TYPE
            AND source_type = :sourceType::SETTLEMENT_TYPE
            AND deleted_at is null
        """
    )
    suspend fun countDestinationBySourceType(destinationId: Long, destinationType: SettlementType, sourceType: SettlementType): Long

    @NewSpan
    @Query(
        """
            SELECT 
                s.source_id
            FROM 
                settlements s
            JOIN 
                account_utilizations au 
                    ON au.document_no = s.destination_id 
                    AND s.destination_type::varchar = au.acc_type::varchar
            WHERE 
                au.document_value ILIKE :query || '%'
                AND s.source_type NOT IN ('CTDS','VTDS','NOSTRO','SECH','PECH')
                AND s.deleted_at is null
                AND au.deleted_at is null
        """
    )
    suspend fun getPaymentIds(query: String): List<Long>

    @NewSpan
    @Query(
        """
            UPDATE settlements SET deleted_at = NOW() WHERE id in (:id) 
        """
    )
    suspend fun deleleSettlement(id: List<Long>)

    @NewSpan
    @Query(
        """
          SELECT id FROM settlements WHERE source_id = :sourceId AND destination_id = :destinationId AND deleted_at is null
           
        """
    )
    suspend fun getSettlementByDestinationId(destinationId: Long, sourceId: Long): List<Long>

    @NewSpan
    @Query(
        """
           SELECT
                p.entity_code, p.bank_id, p.bank_name, p.trans_ref_number,
                p.pay_mode, s.settlement_date::TIMESTAMP
           FROM
                settlements s
                LEFT JOIN payments p  on p.payment_num = s.source_id WHERE
                S.destination_id = :documentNo
                And (p.acc_mode = 'AP' OR p.acc_mode IS NULL)
                AND s.destination_type in ('PINV','PREIMB')
                AND s.source_type not in ('VTDS')
                order by s.created_at desc
           LIMIT 1
        """
    )
    suspend fun getPaymentDetailsByPaymentNum(documentNo: Long?): PaymentInfo?

    @NewSpan
    @Query(
        """
        SELECT
	p.trans_ref_number as document_number,
	s.settlement_date::TIMESTAMP,
	s.source_type,
	s.source_id
FROM
	settlements s
	INNER JOIN payments p ON s.source_id = p.payment_num
WHERE
	s.source_id in (:documentNo)
	AND source_type NOT in('CTDS')
	And(p.acc_mode = 'AR'
		OR p.acc_mode IS NULL)
    AND s.destination_type in ('SINV','SREIMB')
        AND s.source_type = 'REC'
ORDER BY
	s.created_at DESC
	
        """
    )
    suspend fun getPaymentDetailsInRec(documentNo: List<Long?>): List<PaymentInfoRec>

    @NewSpan
    @Query(
        """
           SELECT
	a.document_value as document_number,
	s.settlement_date::TIMESTAMP,
	s.source_type,
	s.source_id
FROM
	settlements s
	INNER JOIN account_utilizations a ON s.source_id = a.document_no
WHERE
	s.source_id in (:documentNo)
	AND source_type NOT in('CTDS')
	And(a.acc_mode = 'AR'
		OR a.acc_mode IS NULL)
    AND s.destination_type in ('SINV','SREIMB')
    AND s.source_type <> 'REC'
ORDER BY
	s.created_at DESC
          
        """
    )
    suspend fun getKnockOffDocument(documentNo: List<Long?>): List<PaymentInfoRec>

    @NewSpan
    @Query(
        """
              select source_id
              from 
              settlements WHERE 
              destination_id = :documentNo AND source_type not in ('CTDS') AND destination_type in ('SINV','SREIMB')
        """
    )
    suspend fun getSettlementDetails(documentNo: Long?): List<Long>

    @NewSpan
    @Query(
        """
           SELECT
                 settlement_date::TIMESTAMP
           FROM
                settlements s where 
                s.source_id = :documentNo
                AND s.destination_type in ('PINV','PREIMB')
                AND s.source_type not in ('VTDS')
                order by s.created_at desc
           LIMIT 1
        """
    )
    suspend fun getSettlementDateBySourceId(documentNo: Long?): Timestamp
}
