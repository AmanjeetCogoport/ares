package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.model.ICJVUpdateRequest
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import io.micronaut.http.annotation.QueryValue

interface ICJVService {
    suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String

    suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse>

    suspend fun getJournalVoucherByParentJVId(@QueryValue("parentId") parentId: String): List<JournalVoucherResponse>

    suspend fun updateICJV(request: ICJVUpdateRequest): String

}
