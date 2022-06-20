package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.PaymentSequenceNumbers
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import javax.transaction.Transactional

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class PaymentNumGeneratorRepo : CoroutineCrudRepository<PaymentSequenceNumbers, Int> {

    abstract suspend fun findBySequenceTypeForUpdate(sequenceType: String): PaymentSequenceNumbers

    @Transactional
    suspend fun getNextSequenceNumber(sequenceType: String): Long {
        var sequenceNumber: PaymentSequenceNumbers = findBySequenceTypeForUpdate(sequenceType)

        val paymentNumber = sequenceNumber.nextSequenceNumber

        sequenceNumber.nextSequenceNumber += 1

        update(sequenceNumber)

        return paymentNumber
    }
}
