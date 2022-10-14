package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)

interface GetInformationRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @Query("select current_date - min(due_date) as curr_outstanding_on_day_of_computation from account_utilizations au where amount_loc - pay_loc != 0 and document_no in (:req)")
    suspend fun getDateDiff(req: List<Long>): Long
}
