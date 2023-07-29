package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface CycleExceptionRepo : CoroutineCrudRepository<CycleExceptions, Long> {

    suspend fun saveAll(paymentDetails: Iterable<CycleExceptions>): List<CycleExceptions>

    @NewSpan
    @Query(
        """
            UPDATE dunning_cycle_exceptions SET deleted_at = NOW(), updated_at = NOW()  WHERE dunning_cycle_id = :cycleId
             AND trade_party_detail_id IN (:tradePartyDetailIds) AND deleted_at IS NULL
        """
    )
    suspend fun deleteExceptionByCycleId(cycleId: Long, tradePartyDetailIds: MutableList<UUID>): Long

    @NewSpan
    @Query(
        """
            SELECT * FROM dunning_cycle_exceptions where dunning_cycle_id = :cycleId and deleted_at IS NULL
        """
    )

    suspend fun getActiveExceptionsByCycle(cycleId: Long): List<CycleExceptions>?

    @NewSpan
    @Query(
        """
            SELECT trade_party_detail_id FROM dunning_cycle_exceptions where dunning_cycle_id = :cycleId AND deleted_at IS NULL
        """
    )
    suspend fun getActiveTradePartyDetailIds(cycleId: Long): List<UUID>

    @NewSpan
    @Query(
        """
       SELECT
	(array_agg(au.organization_name)) [1] AS trade_party_name,
	ce.registration_number,
	ce.trade_party_detail_id,
	SUM(
		CASE WHEN au.acc_type IN('SINV', 'SCN') THEN
			au.sign_flag * (au.amount_loc - au.pay_loc)
		ELSE
			0
		END) AS total_outstanding,
	SUM(
		CASE WHEN au.acc_type IN('REC', 'CTDS') THEN
			au.sign_flag * (au.amount_loc - au.pay_loc)
		ELSE
			0
		END) AS total_on_account,
	(array_agg(led_currency)) [1] AS currency
FROM
	dunning_cycle_exceptions ce
	INNER JOIN account_utilizations au ON ce.trade_party_detail_id = au.organization_id
WHERE
	ce.dunning_cycle_id = :cycleId
	AND ce.deleted_at IS NULL
	AND au.document_status = 'FINAL'
	AND au.deleted_at IS NULL
	AND au.acc_mode = 'AR'
	AND au.acc_type != 'NEWPR'
	AND(:query IS NULL
		OR au.organization_name ILIKE :query
		OR ce.registration_number ILIKE :query)
GROUP BY
	ce.registration_number,
	ce.trade_party_detail_id OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
LIMIT :pageSize
        """
    )
    suspend fun listExceptionByCycleId(
        query: String?,
        cycleId: Long,
        pageSize: Int,
        pageIndex: Int
    ): List<CycleWiseExceptionResp>

    @NewSpan
    @Query(
        """
        SELECT
	COUNT(DISTINCT au.organization_name)
FROM
	dunning_cycle_exceptions ce
	INNER JOIN account_utilizations au ON ce.trade_party_detail_id = au.organization_id
WHERE
	ce.dunning_cycle_id = :cycleId
	AND ce.deleted_at IS NULL
	AND au.document_status = 'FINAL'
	AND au.deleted_at IS NULL
	AND au.acc_mode = 'AR'
	AND au.acc_type != 'NEWPR'
	AND :query IS NULL
	OR au.organization_name ILIKE :query 
        """
    )

    suspend fun getListExceptionByCycleIdTotalCount(
        query: String?,
        cycleId: Long,
    ): Long
}
