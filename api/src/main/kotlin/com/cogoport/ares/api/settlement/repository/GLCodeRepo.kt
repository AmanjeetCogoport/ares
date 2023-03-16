package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.GLCodes
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface GLCodeRepo : CoroutineCrudRepository<GLCodes, Long> {

    @NewSpan
    @Query(
        """
        SELECT * FROM gl_codes where entity_code = :entityCode
    """
    )
    fun getGLCodes(entityCode: Int): List<GLCodes>
}
