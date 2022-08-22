package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.sql.Timestamp
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
            j.trade_partner_name,
            j.created_at,
            j.created_by,
            j.updated_at,
            j.updated_by
            FROM journal_vouchers j
            where 
                (:entityCode is null OR entity_code = :entityCode) AND
                (:startDate is null OR  created_at >= :startDate) AND
                (:status is null OR  status = :status::JV_STATUS) AND
                (:endDate is null OR created_at <= :endDate)
                OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getListVouchers(entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, page: Int, pageLimit: Int,status: JVStatus?, query: String?,): List<JournalVoucher>

    @Query(
        """
        SELECT count(1)
            FROM journal_vouchers j
            where 
                (:entityCode is null OR entity_code = :entityCode) AND
                (:startDate is null OR  created_at >= :startDate) AND
                (:endDate is null OR created_at <= :endDate)
        """
    )
    fun countDocument(entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?): Long

    @Query(
        """
        UPDATE journal_vouchers SET status = :status, updated_by = :performedBy where id = :id
    """
    )
    suspend fun updateStatus(id: Long, status: JVStatus, performedBy: UUID?)
}
