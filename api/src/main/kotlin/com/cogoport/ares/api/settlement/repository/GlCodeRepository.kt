package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.GlCode
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface GlCodeRepository : CoroutineCrudRepository<GlCode, Long> {

    @NewSpan
    @Query(
        """
                SELECT 
                    id,
                    entity_code,
                    account_number,
                    bank_name,
                    currency,
                    gl_code,
                    bank_short_name,
                    created_at
                FROM 
                    gl_codes
                WHERE 
                    (:entityCode IS NULL OR entity_code = :entityCode) and
                    (:accountNumber IS NULL OR account_number = :accountNumber) and 
                    (:currency IS NULL OR currency = :currency)
                AND
                    gl_code ILIKE :q
                LIMIT
                    :pageLimit
            """
    )
    fun getGLCode(entityCode: Int?, q: String?, pageLimit: Int?, accountNumber: String?, currency: String?): List<GlCode>
}
