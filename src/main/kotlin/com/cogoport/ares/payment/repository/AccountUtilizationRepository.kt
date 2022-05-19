package com.cogoport.ares.payment.repository

import com.cogoport.ares.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository:CoroutineCrudRepository<AccountUtilization,Long> {

    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType)")
    suspend fun isDocumentNumberExists(documentNo:Long,accType:String): Boolean

    @Query("delete from account_utilizations where document_no=:documentNo and acc_type=:accType")
    suspend fun deleteInvoiceUtils(documentNo: Long,accType: String):Int
}