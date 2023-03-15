package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface GLCodeRepo : CoroutineCrudRepository<ParentJournalVoucher, Long> {

    @NewSpan
    @Query("""
        SELECT gl_code from gl_codes where entity_code = :entityCode
    """)
    fun getGLCodes(entityCode: Int): List<String>

}
