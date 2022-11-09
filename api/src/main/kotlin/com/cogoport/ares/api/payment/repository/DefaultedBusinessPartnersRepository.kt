package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.DefaultedBusinessPartners
import com.cogoport.ares.model.payment.response.DefaultedBusinessPartnersResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DefaultedBusinessPartnersRepository : CoroutineCrudRepository<DefaultedBusinessPartners, Long> {

    @Query("DELETE FROM defaulted_business_partners WHERE id = :id")
    suspend fun delete(id: Long): Long

    @Query(
        """
            SELECT
            id, business_name,trade_party_detail_serial_id,sage_org_id
            FROM defaulted_business_partners
            WHERE
            (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id::text iLIKE :q)
            ORDER BY created_at DESC
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
        """
    )
    suspend fun getDefaultedBusinessPartners(q: String?, page: Int?, pageLimit: Int?): List<DefaultedBusinessPartnersResponse?>

    @Query(
        """
        SELECT count(*) FROM
        defaulted_business_partners
        WHERE
        (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id::text iLIKE :q)
       """
    )
    suspend fun getCount(q: String?): Long?

    @Query("SELECT EXISTS(SELECT trade_party_detail_serial_id FROM defaulted_business_partners WHERE trade_party_detail_serial_id = :tradePartyDetailSerialId)")
    suspend fun checkIfTradePartyDetailSerialIdExists(tradePartyDetailSerialId: Long): Boolean
}
