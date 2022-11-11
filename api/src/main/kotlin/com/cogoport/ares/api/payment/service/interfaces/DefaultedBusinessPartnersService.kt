package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DefaultedBusinessPartnerRequest
import com.cogoport.ares.model.payment.request.ListDefaultedBusinessPartnersRequest
import com.cogoport.ares.model.payment.response.DefaultedBusinessPartnersResponse
import java.util.UUID

interface DefaultedBusinessPartnersService {

    suspend fun add(request: DefaultedBusinessPartnerRequest): Long

    suspend fun delete(id: Long): Long

    suspend fun list(request: ListDefaultedBusinessPartnersRequest): ResponseList<DefaultedBusinessPartnersResponse?>

    suspend fun listTradePartyDetailIds(): List<UUID>?
}
