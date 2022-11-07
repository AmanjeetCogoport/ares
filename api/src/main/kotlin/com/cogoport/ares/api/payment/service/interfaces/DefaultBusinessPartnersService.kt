package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.BprRequest
import com.cogoport.ares.model.payment.request.ListBprRequest
import com.cogoport.ares.model.payment.response.DefaultBusinessPartnersResponse

interface DefaultBusinessPartnersService {

    suspend fun add(request: BprRequest): Long

    suspend fun delete(id: Long): Long

    suspend fun list(request: ListBprRequest): ResponseList<DefaultBusinessPartnersResponse?>
}
