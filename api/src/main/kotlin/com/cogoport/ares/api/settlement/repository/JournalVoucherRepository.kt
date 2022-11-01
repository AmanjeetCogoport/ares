package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalVoucherRepository : CoroutineCrudRepository<JournalVoucher, Long> {

    @Query(
        """
            SELECT 
            j.id,
            j.entity_id,
            j.entity_code,
            j.jv_num,
            j.type, 
            j.category,
            j.validity_date,
            j.currency,
            j.led_currency,
            j.amount,
            j.status,
            j.exchange_rate,
            j.trade_party_id,
            j.trade_party_name,
            j.created_at,
            j.created_by,
            j.updated_at,
            j.updated_by,
            j.description as description,
            j.acc_mode
            FROM journal_vouchers j
            where 
                (:status is null OR  status = :status::JV_STATUS) AND
                (:category is null OR  category = :category::JV_CATEGORY) AND
                (:type is null OR  type = :type) AND
                (:query is null OR trade_party_name ilike '%'||:query||'%' OR jv_num ilike '%'||:query||'%')
                OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getListVouchers(
        status: JVStatus?,
        category: JVCategory?,
        type: String?,
        query: String?,
        page: Int,
        pageLimit: Int
    ): List<JournalVoucher>

    @Query(
        """
        SELECT count(1)
            FROM journal_vouchers j
            where 
                (:status is null OR  status = :status::JV_STATUS) AND
                (:category is null OR  category = :category::JV_CATEGORY) AND
                (:type is null OR  type = :type) AND
                (:query is null OR trade_party_name ilike '%'||:query||'%' or jv_num ilike '%'||:query||'%')
        """
    )
    fun countDocument(status: JVStatus?, category: JVCategory?, type: String?, query: String?): Long

    @Query(
        """
        UPDATE journal_vouchers 
        SET status = 'REJECTED', updated_by = :performedBy, description = :remark, updated_at = NOW() 
        WHERE id = :id
    """
    )
    suspend fun reject(id: Long, performedBy: UUID, remark: String?)

    @Query(
        """
            UPDATE journal_vouchers 
            SET status = :status, updated_by = :performedBy, updated_at = NOW() 
            WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: JVStatus, performedBy: UUID)
}
