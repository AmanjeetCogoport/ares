package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.JournalVoucherMigration
import com.cogoport.ares.model.payment.AccMode
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface JournalVoucherRepoMigration : CoroutineCrudRepository<JournalVoucherMigration, Long> {

    @Query(
        """
            select a.id 
            from account_utilizations a 
            inner join journal_vouchers jv 
            on (jv.id = a.document_no and jv.jv_num = a.document_value)
            where jv.jv_num = :jvNum
            and jv.sage_unique_id = :sageUniqueId
            and jv.acc_mode = :accMode::account_mode
            and jv.trade_party_id = :tradePartyDetailId
        """
    )
    suspend fun getAccUtilId(
        sageUniqueId: String,
        jvNum: String,
        accMode: AccMode,
        tradePartyDetailId: UUID
    ): Long?
}
