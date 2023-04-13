package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.JvCategory

interface JvCategoryService {

    suspend fun getJvCategory(q: String?, pageLimit: Int?): List<JvCategory>
}
