package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.GLCode
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface GLCodeRepo : CoroutineCrudRepository<GLCode, Long> {

    @NewSpan
    @Query(
        """
        SELECT id,
         entity_code,
         account_number,
         bank_name,
         currency,
         gl_code,
         bank_short_name,
         created_at
         FROM gl_codes where entity_code = :entityCode
    """
    )
    fun getGLCodes(entityCode: Int): List<GLCode>
}
