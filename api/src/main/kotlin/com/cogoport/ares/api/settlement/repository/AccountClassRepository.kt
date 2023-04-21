package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.AccountClass
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountClassRepository : CoroutineCrudRepository<AccountClass, Long> {

    @NewSpan
    @Query(
        """
                SELECT 
                    * 
                FROM 
                    account_classes 
                WHERE 
                    led_account = :ledAccount 
                AND 
                    class_code = :accountCode
            """
    )
    fun getAccountClass(ledAccount: String, accountCode: Int): AccountClass
}
