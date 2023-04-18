package com.cogoport.ares.api.utils

import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.model.common.GetPartnerRequest
import jakarta.inject.Inject
import java.util.UUID

class Util {
    @Inject
    lateinit var cogoBackClient: RailsClient

    public suspend fun getCogoEntityCode(cogoEntityId: String?): String? {
        if (cogoEntityId == null) {
            return null
        }
        val data = cogoBackClient.getPartnerDetails(GetPartnerRequest(UUID.fromString(cogoEntityId), false)) as LinkedHashMap<String, LinkedHashMap<String, String>>
        return data.get("data")?.get("entity_code")
    }

    fun toQueryString(q: String?): String {
        return if (q == null) "%%" else "%${q}%"
    }
}
