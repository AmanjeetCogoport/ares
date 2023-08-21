package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalVoucherRepository : CoroutineCrudRepository<JournalVoucher, Long> {

    @NewSpan
    @Query(
        """
            UPDATE 
                journal_vouchers 
            SET 
                status = :status, 
                updated_by = :performedBy, 
                updated_at = NOW() 
            WHERE 
                parent_jv_id = :id
            AND 
                deleted_at IS NULL
        """
    )
    suspend fun updateStatus(id: Long, status: JVStatus, performedBy: UUID)

    @NewSpan
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
                (j.created_at at time zone 'utc' at time zone 'Asia/Kolkata') as created_at,
                j.created_by,
                (j.updated_at at time zone 'utc' at time zone 'Asia/Kolkata') as updated_at,
                j.updated_by,
                j.description as description,
                j.acc_mode,
                j.parent_jv_id,
                j.sage_unique_id,
                j.migrated,
                j.gl_code,
                j.led_amount,
                j.sign_flag,
                j.deleted_at,
                j.additional_details
            FROM 
                journal_vouchers j 
            Where 
                j.parent_jv_id = :parentId
            AND 
                j.deleted_at IS NULL
        """
    )
    suspend fun getJournalVoucherByParentJVId(parentId: Long): List<JournalVoucher>

    @NewSpan
    @Query(
        """
                UPDATE 
                    journal_vouchers 
                SET 
                    deleted_at = NOW(),
                    status = 'DELETED'::JV_STATUS,
                    updated_at = NOW(),
                    updated_by = :performedBy
                WHERE 
                    parent_jv_id = :parentJvId 
                AND
                    deleted_at IS NULL
            """
    )
    suspend fun deleteJvLineItemByParentJvId(parentJvId: Long, performedBy: UUID)

    @NewSpan
    @Query(
        """
                DELETE 
                FROM 
                    journal_vouchers 
                WHERE 
                    parent_jv_id = :parentJVId
            """
    )
    suspend fun deletingLineItemsWithParentJvId(parentJVId: Long)

    suspend fun saveAll(req: List<JournalVoucher>): List<JournalVoucher>

    @NewSpan
    @Query(
        """
        select * from journal_vouchers where additional_details ->> 'utr' = :utr  and acc_mode != 'OTHER' and acc_mode is not null
    """
    )
    suspend fun findByDescription(utr: String): JournalVoucher?

    @NewSpan
    @Query(
        """
        SELECT * FROM journal_vouchers WHERE jv_num IN (:jvNums)
    """
    )
    suspend fun findByJvNums(jvNums: List<String>): List<JournalVoucher>?
}
