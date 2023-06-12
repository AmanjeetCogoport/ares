package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface CycleExceptionRepo : CoroutineCrudRepository<CycleExceptions, Long> {

    suspend fun saveAll(paymentDetails: Iterable<CycleExceptions>): List<CycleExceptions>

    @Query(
        """
            UPDATE dunning_cycle_exceptions SET deleted_at = NOW() WHERE cycle_id = :cycleId
             AND trade_party_detail_id NOT IN (:detailsIds) AND deleted_at IS NULL
        """
    )
    suspend fun deleteExceptionByCycleId(cycleId: Long, detailsIds: MutableList<UUID>)

    @Query(
        """
            SELECT * FROM dunning_cycle_exceptions where cycle_id = :cycleId and deleted_at IS NULL
        """
    )

    suspend fun getActiveExceptionsByCycle(cycleId: Long): List<CycleExceptions>?

    @Query(
        """
           SELECT au.organization_name , ce.registration_number , ce.trade_party_detail_id ,
            SUM(CASE WHEN au.acc_type IN ('SINV','SCN') THEN au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) as total_outstanding,
            SUM(CASE WHEN au.acc_type IN ('REC','CTDS') THEN au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) as total_on_account
            FROM dunning_cycle_exceptions ce INNER JOIN account_utilizations au ON ce.trade_party_detail_id = au.organization_id
            WHERE ce.cycle_id = :cycleId
            AND ce.deleted_at IS NULL
            AND au.document_status = 'FINAL'
            AND au.deleted_at IS NULL
            AND au.acc_mode = 'AR'
	        AND au.acc_type != 'NEWPR' 
            AND :query IS NULL OR au.organization_name ILIKE :query
        group by au.organization_name,ce.registration_number,ce.trade_party_detail_id   
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
        """
    )
    suspend fun listExceptionByCycleId(
        query: String?,
        cycleId: Long,
        pageIndex: Int,
        pageSize: Int
    ): List<CycleWiseExceptionResp>

    @Query(
        """
        SELECT COUNT(DISTINCT au.organization_name)
            FROM dunning_cycle_exceptions ce INNER JOIN account_utilizations au ON ce.trade_party_detail_id = au.organization_id
            WHERE ce.cycle_id = :cycleId
            AND ce.deleted_at IS NULL
            AND au.document_status = 'FINAL'
            AND au.deleted_at IS NULL
            AND au.acc_mode = 'AR'
	        AND au.acc_type != 'NEWPR'
            AND :query IS NULL OR au.organization_name ILIKE :query   
        """
    )

    suspend fun getListExceptionByCycleIdTotalCount(
        query: String?,
        cycleId: Long,
    ): Long
}
