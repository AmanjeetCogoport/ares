package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.SuspenseAccount
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SuspenseAccountRepo : CoroutineCrudRepository<SuspenseAccount, Long> {

    @Query(
        """
            SELECT * FROM suspense_accounts where id = :id and is_deleted is false
        """
    )
    fun findBySuspenseId(id: Long): SuspenseAccount?
}
