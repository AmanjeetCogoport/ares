package com.cogoport.ares.gateway

import com.cogoport.ares.common.AresConstants
import com.cogoport.ares.exception.AresError
import com.cogoport.ares.exception.AresException
import com.cogoport.brahma.opensearch.Client
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest

class OpenSearchClient {

    fun <T : Any> response(searchKey: String, classType: Class<T>, index: String = AresConstants.SALES_DASHBOARD_INDEX, key: String = AresConstants.OPEN_SEARCH_DOCUMENT_KEY): T? {

        val response = Client.search(
            { s: SearchRequest.Builder ->
                s.index(index)
                    .query { q: Query.Builder ->
                        q.match { t: MatchQuery.Builder ->
                            t.field(key).query(FieldValue.of(searchKey))
                        }
                    }
            },
            classType
        )
        var outResp: T? = null
        if(response?.hits()?.total()?.value() == 0.toLong())
            throw AresException(AresError.ERR_1005, "")
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }
}
