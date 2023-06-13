package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DunningCycleExceptionRepo : CoroutineCrudRepository<DunningCycleExecution, Long>{

    @Query(
        """
            UPDATE dunning_cycle_executions SET status = :status WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long,status: String)
}
