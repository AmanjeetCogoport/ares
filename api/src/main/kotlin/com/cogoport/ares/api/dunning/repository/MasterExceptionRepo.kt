package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface MasterExceptionRepo : CoroutineCrudRepository<MasterExceptions, Long> {

    suspend fun saveAll(paymentDetails: Iterable<MasterExceptions>): List<MasterExceptions>

    @Query(
        """
             SELECT
                me.id,
                me.is_active,
                me.trade_party_name AS name,
                me.registration_number,
                me.org_segment,
                me.credit_days,
                me.credit_amount,
                SUM((amount_loc - pay_loc) * sign_flag) AS total_due_amount
            FROM
                account_utilizations au
                JOIN dunning_master_exceptions me ON me.trade_party_detail_id = au.organization_id
            WHERE
                me.deleted_at IS NULL
                AND au.document_status = 'FINAL'
                AND au.acc_mode = 'AR'
                AND acc_type != 'NEWPR'
                AND au.deleted_at IS NULL
                AND :query IS NULL OR me.trade_party_name ILIKE :query OR me.registration_number ILIKE :query
                AND :segment IS NULL OR me.org_segment::VARCHAR = :segment
                AND (:creditDateFrom IS NULL OR :creditDaysTo IS NULL OR me.credit_days BETWEEN :creditDateFrom AND :creditDaysTo)
            GROUP BY
                me.id
            ORDER BY
                CASE WHEN :sortType = 'ASC' AND :sortBy = 'dueAmount' THEN SUM((amount_loc - pay_loc) * sign_flag) END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortBy = 'dueAmount' THEN SUM((amount_loc - pay_loc) * sign_flag) END DESC,
                CASE WHEN :sortType = 'ASC' AND :sortBy = 'creditDays' THEN me.credit_days END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortBy = 'creditDays' THEN me.credit_days END DESC,
                CASE WHEN :sortType = 'ASC' AND :sortBy = 'creditAmount' THEN me.credit_amount END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortBy = 'creditAmount' THEN me.credit_amount END DESC 
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize  
        
    """
    )
    suspend fun listMasterException(
        query: String?,
        segment: String?,
        pageIndex: Int,
        pageSize: Int,
        creditDateFrom: Long?,
        creditDaysTo: Long?,
        sortBy: String,
        sortType: String
    ): List<MasterExceptionResp>

    @Query(
        """
            SELECT
            COALESCE(count(DISTINCT me.id),0)
        FROM
            account_utilizations au
            JOIN dunning_master_exceptions me ON me.trade_party_detail_id = au.organization_id
        WHERE
            me.deleted_at IS NULL
            AND au.document_status = 'FINAL'
            AND au.acc_mode = 'AR'
            AND acc_type != 'NEWPR'
            AND au.deleted_at IS NULL
            AND (:query IS NULL OR me.trade_party_name ILIKE :query OR me.registration_number ILIKE :query)
            AND :segment IS NULL OR me.org_segment::VARCHAR = :segment          
        """
    )

    suspend fun listMasterExceptionTotalCount(
        query: String?,
        segment: String?
    ): Long

    @Query(
        """
            SELECT * FROM dunning_master_exceptions where deleted_at IS NULL
        """
    )
    suspend fun getActiveMasterExceptions(): List<MasterExceptions>?

    @Query(
        """
             UPDATE dunning_master_exceptions
                SET deleted_at = CASE
                   WHEN :actionType = 'DELETE' THEN NOW()
                   ELSE deleted_at
               END,
            is_active = CASE
                   WHEN :actionType != 'DELETE' THEN NOT is_active
                   ELSE is_active
               END,
            updated_at = NOW(),
            updated_by = :updatedBy::UUID
            WHERE id = :id
        """
    )
    suspend fun deleteOrUpdateException(id: Long, updatedBy: UUID, actionType: String)
}
