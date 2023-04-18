package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface ParentJVRepository : CoroutineCrudRepository<ParentJournalVoucher, Long> {

    @NewSpan
    @Query(
        """
            SELECT 
                pjv.id,
                pjv.jv_num,
                pjv.category,
                pjv.status,
                pjv.entity_code,
                pjv.description,
                pjv.currency,
                pjv.led_currency,
                pjv.exchange_rate,
                pjv.migrated,
                pjv.jv_code_num,
                pjv.transaction_date,
                pjv.validity_date,
                pjv.created_at,
                pjv.created_by,
                pjv.updated_at,
                pjv.updated_by,
                pjv.deleted_at,
                pjv.is_utilized
            FROM 
                parent_journal_vouchers pjv
            WHERE 
                (:status IS NULL OR pjv.status = :status::JV_STATUS) 
            AND
                (:category IS NULL OR pjv.category = :category::VARCHAR) 
            AND
                ((:query IS NULL OR pjv.jv_num ILIKE '%'||:query||'%') OR
                ((pjv.id IN (SELECT parent_jv_id FROM journal_vouchers jv WHERE (jv.trade_party_name ILIKE '%'||:query||'%')))))
            AND
                pjv.deleted_at is NULL
            ORDER BY
                CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'createdAt' THEN pjv.created_at                         
                         WHEN :sortBy = 'accountingDate' THEN pjv.transaction_date
                    END
                END 
                Desc,
                CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'createdAt' THEN pjv.created_at
                         WHEN :sortBy = 'accountingDate' THEN pjv.transaction_date
                    END        
                END 
                Asc
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getListVouchers(
        status: JVStatus?,
        category: String?,
        query: String?,
        page: Int,
        pageLimit: Int,
        sortType: String?,
        sortBy: String?
    ): List<ParentJournalVoucher>

    @NewSpan
    @Query(
        """
        SELECT count(1)
        FROM
            parent_journal_vouchers j
        WHERE 
            (:status IS NULL OR  status = :status::JV_STATUS) 
        AND
            (:category IS NULL OR  category = :category::VARCHAR) 
        AND
            (:query IS NULL OR jv_num ILIKE '%'||:query||'%')
        """
    )
    fun countDocument(status: JVStatus?, category: String?, query: String?): Long

    @NewSpan
    @Query(
        """
                UPDATE 
                    parent_journal_vouchers 
                SET 
                    deleted_at = NOW(),
                    status = 'DELETED'::JV_STATUS,
                    updated_at = NOW(),
                    updated_by = :performedBy
                WHERE 
                    id = :id 
                AND
                    deleted_at IS NULL
            """
    )
    suspend fun deleteJournalVoucherById(id: Long, performedBy: UUID)

    @NewSpan
    @Query(
        """
            UPDATE
                parent_journal_vouchers
            SET 
                status = :status, 
                updated_by = :performedBy, 
                updated_at = NOW() 
            WHERE 
                id = :id
            AND
                deleted_at IS NULL
        """
    )
    suspend fun updateStatus(id: Long, status: JVStatus, performedBy: UUID)
}
