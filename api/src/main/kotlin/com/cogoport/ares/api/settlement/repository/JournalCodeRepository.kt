package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JournalCode
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalCodeRepository : CoroutineCrudRepository<JournalCode, Long> {

    @NewSpan
    @Query(
        """
                SELECT
                    id,
                    number,
                    description,
                    created_at,
                    updated_at
                FROM 
                    journal_voucher_codes 
                WHERE 
                   number ILIKE :q
                LIMIT 
                    :pageLimit
            """
    )
    fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode>
}
