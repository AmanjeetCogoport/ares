package com.cogoport.ares.api.utils

import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.model.common.GetPartnerRequest
import jakarta.inject.Inject
import java.math.BigDecimal
import java.math.RoundingMode
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
        return if (q == null) "%%" else "%$q%"
    }

    fun replaceUnderScore(request: String): String {
        return request.replace("_", " ").uppercase().replace(" ", "_")
    }

    companion object {
        fun BigDecimal.divideNumbers(num: BigDecimal): BigDecimal {
            return if (num.compareTo(0.toBigDecimal()) > 0) {
                this.divide(num, 4, RoundingMode.UP)
            } else {
                0.toBigDecimal()
            }
        }
    }
}
