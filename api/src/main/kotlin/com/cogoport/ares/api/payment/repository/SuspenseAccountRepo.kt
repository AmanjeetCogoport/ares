package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.SuspenseAccount
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.sql.Timestamp

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface SuspenseAccountRepo : CoroutineCrudRepository<SuspenseAccount, Long> {

    @Query(
        """
            SELECT * FROM suspense_accounts WHERE id = :id AND is_deleted IS FALSE
        """
    )
    fun findBySuspenseId(id: Long?): SuspenseAccount

    @Query(
        """ SELECT * FROM suspense_accounts
            WHERE (:entityType IS NULL OR entity_code = :entityType)
            AND (:currencyType IS NULL OR currency = :currencyType)
            AND ( (:startDate IS NULL AND :endDate IS NULL) OR 
                (transaction_date >= :startDate AND  transaction_date <= :endDate) )
            AND (:query IS NULL OR trans_ref_number ILIKE :query || '%')
            AND payment_id IS NULL
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    fun getSuspenseAccounts(entityType: Int?, startDate: Timestamp?, endDate: Timestamp?, currencyType: String?, page: Int, pageLimit: Int, query: String?): List<SuspenseAccount>

    @Query(
        """
            SELECT count(*) FROM suspense_accounts
       WHERE (:entityType IS NULL OR entity_code = :entityType)
            AND (:currencyType IS NULL OR currency = :currencyType)
            AND ( (:startDate IS NULL AND :endDate IS NULL) OR 
                (transaction_date >= :startDate AND  transaction_date <= :endDate) )
            AND (:query IS NULL OR trans_ref_number ILIKE :query || '%')
            AND payment_id IS NULL
        """
    )
    fun getSuspenseCount(entityType: Int?, startDate: Timestamp?, endDate: Timestamp?, currencyType: String?, page: Int, pageLimit: Int, query: String?): Int
}
