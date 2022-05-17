package com.cogoport.ares.payment.repository

import com.cogoport.ares.payment.entity.Payment
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    suspend fun listOrderByCreatedAtDesc(): MutableList<Payment>
}
