package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.JvCategory
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JvCategoryRepository : CoroutineCrudRepository<JvCategory, Long> {

    @NewSpan
    @Query(
        """
                SELECT
                    id,
                    category::varchar,
                    description,
                    created_at,
                    updated_at
                FROM 
                    journal_voucher_categories 
                WHERE 
                    category ILIKE :q
                LIMIT 
                    :pageLimit
            """
    )
    fun getJvCategory(q: String?, pageLimit: Int?): List<JvCategory>
}
