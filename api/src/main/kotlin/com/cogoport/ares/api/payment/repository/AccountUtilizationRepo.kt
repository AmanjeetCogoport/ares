package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepo : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET tagged_settlement_id = :taggedSettlementIds WHERE document_no = :documentNo
            and acc_mode = 'AP' and is_draft = false and deleted_at is null
        """
    )
    suspend fun updateTaggedSettlementIds(documentNo: Long, taggedSettlementIds: String)
}
