package com.cogoport.ares.payment.repository

import com.cogoport.ares.payment.entity.AccountUtilization
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository:CoroutineCrudRepository<AccountUtilization,Long> {
}