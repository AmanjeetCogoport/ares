package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Bpr
import com.cogoport.ares.model.payment.response.BprResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface BprRepository : CoroutineCrudRepository<Bpr, Long> {

    @Query("DELETE from bpr where id = :id")
    suspend fun delete(id: Long): Long

    @Query(
        """
            SELECT
            id, business_name,trade_party_detail_serial_id,sage_org_id
            FROM bpr
            WHERE
            (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id iLIKE :q)
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getBpr(q: String?, page: Int?, pageLimit: Int?): List<BprResponse?>

    @Query(
        """
        SELECT count(*) FROM
        (
        SELECT
        id, business_name,trade_party_detail_serial_id,sage_org_id
        FROM bpr
        WHERE
        (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id iLIKE :q)
        ) as output
    """
    )
    suspend fun getCount(q: String?): Long?
}
