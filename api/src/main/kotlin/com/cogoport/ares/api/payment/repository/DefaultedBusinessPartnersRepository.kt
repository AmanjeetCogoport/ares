package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.DefaultedBusinessPartners
import com.cogoport.ares.model.payment.response.DefaultedBusinessPartnersResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DefaultedBusinessPartnersRepository : CoroutineCrudRepository<DefaultedBusinessPartners, Long> {

    @NewSpan
    @Query("DELETE FROM defaulted_business_partners WHERE id = :id")
    suspend fun delete(id: Long): Long

    @NewSpan
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

    @NewSpan
    @Query(
        """
        SELECT count(*) FROM
        defaulted_business_partners
        WHERE
        (:q IS NULL OR business_name iLIKE :q OR trade_party_detail_serial_id::text iLIKE :q)
       """
    )
    suspend fun getCount(q: String?): Long?

    @NewSpan
    @Query("SELECT EXISTS(SELECT trade_party_detail_serial_id FROM defaulted_business_partners WHERE trade_party_detail_serial_id = :tradePartyDetailSerialId)")
    suspend fun checkIfTradePartyDetailSerialIdExists(tradePartyDetailSerialId: Long): Boolean

    @NewSpan
    @Query("SELECT trade_party_detail_id FROM defaulted_business_partners")
    suspend fun listTradePartyDetailIds(): List<UUID>?
}
