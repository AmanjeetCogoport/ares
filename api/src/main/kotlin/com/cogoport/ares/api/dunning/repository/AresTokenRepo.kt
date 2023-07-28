package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.AresTokens
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AresTokenRepo : CoroutineCrudRepository<AresTokens, Long> {

    suspend fun saveAll(paymentDetails: Iterable<AresTokens>): List<AresTokens>

    @NewSpan
    @Query(
        """
        SELECT * FROM tokens WHERE token = :token AND token_type::VARCHAR = :tokenType
    """
    )
    suspend fun findByTokens(token: String, tokenType: String): AresTokens?
}
