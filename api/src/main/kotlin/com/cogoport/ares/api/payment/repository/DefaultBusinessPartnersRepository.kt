package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.DefaultBusinessPartners
import com.cogoport.ares.model.payment.response.DefaultBusinessPartnersResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DefaultBusinessPartnersRepository : CoroutineCrudRepository<DefaultBusinessPartners, Long> {

    @Query("DELETE FROM default_business_partners WHERE id = :id")
    suspend fun delete(id: Long): Long

    @Query(
        """
            SELECT
            id, business_name,trade_party_detail_serial_id,sage_org_id
            FROM default_business_partners
            WHERE
            (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id iLIKE :q)
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getDefaultBusinessPartners(q: String?, page: Int?, pageLimit: Int?): List<DefaultBusinessPartnersResponse?>

    @Query(
        """
        SELECT count(*) FROM
        default_business_partners
        WHERE
        (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id iLIKE :q)
       """
    )
    suspend fun getCount(q: String?): Long?

    @Query("SELECT trade_party_detail_serial_id FROM default_business_partners WHERE trade_party_detail_serial_id = :tradePartyDetailSerialId")
    suspend fun findTradePartyDetailSerialId(tradePartyDetailSerialId: String): String
}
