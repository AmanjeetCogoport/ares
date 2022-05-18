package com.cogoport.ares.common.classes

import com.cogoport.ares.common.AresConstants
import com.cogoport.brahma.opensearch.Client
import io.micronaut.validation.Validated
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import java.util.function.Function


class OpenSearch {

    fun <R : Any>response(searchKey: String, classType: Class<R>, index: String = AresConstants.SALES_DASHBOARD_INDEX, key: String = AresConstants.OPEN_SEARCH_DOCUMENT_KEY): R?{

        val response = Client.search(
            Function { s: SearchRequest.Builder ->
                s.index(index)
                    .query { q: Query.Builder ->
                        q.match { t: MatchQuery.Builder ->
                            t.field(key).query(FieldValue.of(searchKey))
                        }
                    }
            },
            classType
        )
        var outResp: R? = null
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }
}