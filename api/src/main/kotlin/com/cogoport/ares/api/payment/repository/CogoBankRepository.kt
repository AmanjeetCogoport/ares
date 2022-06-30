package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.CogoBankDetails
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface CogoBankRepository : CoroutineCrudRepository<CogoBankDetails, Int> {

    @Query(
        """
        select id,bank_id,account_no,bank_name ,entity_code,created_at,updated_at  from cogo_bank_details where account_no =:accountNo
    """
    )
    suspend fun findByAccountNo(accountNo: String): CogoBankDetails
}
