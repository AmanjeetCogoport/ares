package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.model.ICJVUpdateRequest
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentICJVRequest
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

interface ICJVService {
    suspend fun createICJV(request: ParentICJVRequest): String

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse>

    suspend fun getJournalVoucherByParentJVId(@QueryValue("parentId") parentId: Long): List<JournalVoucherResponse>

    suspend fun updateICJV(request: ICJVUpdateRequest): String

    suspend fun postICJVToSage(parentICJVId: Long, performedBy: UUID): Boolean
}
