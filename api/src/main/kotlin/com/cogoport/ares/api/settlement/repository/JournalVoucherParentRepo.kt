package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.opentelemetry.instrumentation.annotations.WithSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalVoucherParentRepo : CoroutineCrudRepository<ParentJournalVoucher, Long> {

    @WithSpan
    @Query(
        """
            SELECT 
            j.id,
            j.jv_num,
            j.category,
            j.status,
            j.validity_date,
            j.created_at,
            j.created_by,
            j.updated_at,
            j.updated_by
            FROM parent_journal_vouchers j
            where 
                (:status is null OR  j.status = :status::JV_STATUS) AND
                (:category is null OR  j.category = :category::JV_CATEGORY) AND
                ((:query is null OR j.jv_num ilike '%'||:query||'%') OR
                ((j.id IN (SELECT parent_jv_id FROM journal_vouchers jv WHERE (jv.trade_party_name ilike '%'||:query||'%')))))
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'createdAt' THEN j.created_at                         
                         WHEN :sortBy = 'validityDate' THEN j.validity_date
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'createdAt' THEN j.created_at
                         WHEN :sortBy = 'validityDate' THEN j.validity_date
                    END        
            END 
            Asc
                OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getListVouchers(
        status: JVStatus?,
        category: JVCategory?,
        query: String?,
        page: Int,
        pageLimit: Int,
        sortType: String?,
        sortBy: String?
    ): List<ParentJournalVoucher>

    @WithSpan
    @Query(
        """
        SELECT count(1)
            FROM parent_journal_vouchers j
            where 
                (:status is null OR  status = :status::JV_STATUS) AND
                (:category is null OR  category = :category::JV_CATEGORY) AND
                (:query is null or jv_num ilike '%'||:query||'%')
        """
    )
    fun countDocument(status: JVStatus?, category: JVCategory?, query: String?): Long
}
