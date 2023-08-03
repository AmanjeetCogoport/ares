package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface MasterExceptionRepo : CoroutineCrudRepository<MasterExceptions, Long> {

    suspend fun saveAll(paymentDetails: Iterable<MasterExceptions>): List<MasterExceptions>

    @NewSpan
    @Query(
        """
           WITH list_data AS (
            SELECT
            me.id,
            me.is_active,
            me.trade_party_name AS name,
            me.registration_number,
            me.organization_segment::VARCHAR AS org_segment,
            me.entity_code,
            COALESCE(SUM((amount_loc - pay_loc) * sign_flag), 0) AS total_due_amount,
            COALESCE((array_agg(led_currency))[1], 'INR') AS currency
        FROM
            dunning_master_exceptions me
        LEFT JOIN
            account_utilizations au ON me.trade_party_detail_id = au.organization_id
            WHERE me.deleted_at IS NULL
        GROUP BY
        me.id
    )
    SELECT *
        FROM list_data
     WHERE
         (:query IS NULL OR name ILIKE :query OR registration_number ILIKE :query)
         AND (:segment IS NULL OR org_segment::VARCHAR = :segment)
         AND (COALESCE(:entities) IS NULL OR entity_code IN (:entities))
    ORDER BY
    CASE WHEN :sortType = 'ASC' AND :sortBy = 'dueAmount' THEN total_due_amount END ASC,
    CASE WHEN :sortType = 'DESC' AND :sortBy = 'dueAmount' THEN total_due_amount END DESC
    OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize   
    """
    )
    suspend fun listMasterException(
        query: String?,
        entities: List<Long>?,
        segment: String?,
        pageIndex: Int,
        pageSize: Int,
        sortBy: String,
        sortType: String
    ): List<MasterExceptionResp>

    @NewSpan
    @Query(
        """
            SELECT
                COALESCE(count(DISTINCT id),0)
            FROM
                dunning_master_exceptions 
            WHERE
                deleted_at IS NULL
                AND (:query IS NULL OR trade_party_name ILIKE :query OR registration_number ILIKE :query)
                AND (:segment IS NULL OR organization_segment::VARCHAR = :segment)
                AND (COALESCE(:entities) IS NULL OR entity_code IN (:entities))
        """
    )
    suspend fun listMasterExceptionTotalCount(
        query: String?,
        entities: List<Long>?,
        segment: String?
    ): Long

    @NewSpan
    @Query(
        """
            SELECT * FROM dunning_master_exceptions where deleted_at IS NULL
        """
    )
    suspend fun getAllMasterExceptions(): List<MasterExceptions>?

    @NewSpan
    @Query(
        """
            SELECT trade_party_detail_id FROM dunning_master_exceptions where deleted_at IS NULL AND is_active = TRUE
        """
    )
    suspend fun getActiveTradePartyDetailIds(): List<UUID>

    @NewSpan
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
