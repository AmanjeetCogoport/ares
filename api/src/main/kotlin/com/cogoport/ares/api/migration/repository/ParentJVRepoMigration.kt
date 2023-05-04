package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.ParentJournalVoucherMigration
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface ParentJVRepoMigration : CoroutineCrudRepository<ParentJournalVoucherMigration, Long> {

    @Query("select id from parent_journal_vouchers where jv_num = :jvNum and category = :category")
    suspend fun checkIfParentJVExists(jvNum: String, category: String): Long?
}
