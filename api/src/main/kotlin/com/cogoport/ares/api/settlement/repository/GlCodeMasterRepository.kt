package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface GlCodeMasterRepository : CoroutineCrudRepository<GlCodeMaster, Long> {

    @NewSpan
    @Query(
        """
                SELECT 
                    id,
                    account_code,
                    description,
                    led_account,
                    acc_mode,
                    class_code,
                    created_by,
                    updated_by,
                    created_at,
                    updated_at
                FROM 
                    gl_code_masters
                WHERE 
                    (:q IS NULL OR account_code::VARCHAR ILIKE '%'||:q||'%')
                AND
                    (:accMode IS NULL OR acc_mode = :accMode::VARCHAR)
                LIMIT 
                    :pageLimit
            """
    )
    fun getGLCodeMaster(accMode: String?, q: String?, pageLimit: Int?): List<GlCodeMaster>
}
