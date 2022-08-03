package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JournalVoucher
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalVoucherRepository : CoroutineCrudRepository<JournalVoucher, Long> {

    @Query(
        """
            SELECT 
            j.id,
            s.source_id,
            s.source_type,
            s.destination_id,
            s.destination_type, 
            s.currency,
            s.amount,
            s.led_currency,
            s.led_amount,
            s.sign_flag,
            s.settlement_date,
            s.created_at,
            s.created_by,
            s.updated_at,
            s.updated_by
            FROM journal_vouchers j
            where destination_id = :destId and destination_type::varchar = :destType
        """
    )
    suspend fun getByList(journalVoucher: JournalVoucher): List<JournalVoucher>
}
