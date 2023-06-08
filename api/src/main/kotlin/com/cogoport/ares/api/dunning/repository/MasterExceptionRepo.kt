package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.CycleExceptions
import com.cogoport.ares.api.dunning.entity.MasterExceptions
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.model.common.ResponseList
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface MasterExceptionRepo: CoroutineCrudRepository<MasterExceptions, Long> {
    @Query(
        """
            select me.id,me.is_active,au.organization_name as name, me.registration_number ,me.segmentation,
            me.credit_days,me.credit_amount,SUM((amount_loc - pay_loc) * sign_flag) AS total_due_amount
            FROM account_utilizations au JOIN dunning_master_exceptions me ON
            me.trade_party_details_id = au.organization_id where me.deleted_at = NULL AND
            au.document_status = 'FINAL'
            AND au.acc_mode = 'AR',
            AND acc_type != 'NEWPR'
            AND au.deleted_at IS NULL
            ORDER BY
                CASE WHEN :sortType = 'ASC' AND :sortField = 'balanceAmount' THEN balance_amount END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortField = 'balanceAmount' THEN balance_amount END DESC
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
        
    """)
    suspend fun listMasterException(
        query: String?,
        segment: String?,
        pageIndex: Int, pageSize: Int, sortField: String, sortType: String
    ): List<MasterExceptionResp>



}