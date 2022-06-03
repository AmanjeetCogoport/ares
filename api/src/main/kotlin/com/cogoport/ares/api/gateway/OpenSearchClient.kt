package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.brahma.opensearch.Client
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import java.sql.Timestamp

class OpenSearchClient {

    fun <T : Any> search(searchKey: String, classType: Class<T>, index: String = AresConstants.SALES_DASHBOARD_INDEX): T? {

        val response = Client.search(
            { s: SearchRequest.Builder ->
                s.index(index)
                    .query { q: Query.Builder ->
                        q.ids { i -> i.values(searchKey) }
                    }
            },
            classType
        )
        var outResp: T? = null
        if (response?.hits()?.total()?.value() == 0.toLong())
            throw AresException(AresError.ERR_1005, "")
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }

    fun <T : Any> searchList(searchKey: String?, classType: Class<T>, index: String = AresConstants.SALES_DASHBOARD_INDEX, offset: Int, limit: Int): SearchResponse<T>? {

        val response: SearchResponse<T>? = Client.search(
            { s ->
                s.index(index)
                    .query { q ->
                        if (!searchKey.isNullOrBlank()) {
                            q.matchPhrase { a -> a.field("organizationId").query(searchKey) }
                        } else {
                            q.matchAll { s -> s.queryName("") }
                        }
                        q
                    }.from(offset).size(limit)
            },
            classType
        )
        return response
    }

    fun <T : Any> listApi(index: String, classType: Class<T>, values: List<String>, offset: Int? = null, limit: Int? = null): SearchResponse<T>? {
        val response = Client.search(
            { s ->
                s.index(index).query { q ->
                    q.ids { i -> i.values(values) }
                }.from(offset).size(limit)
            },
            classType
        )
        return response
    }

    fun <T> updateDocument(index: String, docId: String, docData: T) {
        Client.updateDocument(index, docId, docData)
    }

    fun <T : Any> onAccountSearch(request: AccountCollectionRequest, classType: Class<T>): SearchResponse<T>? {
        val response = Client.search(
            { s ->
                s.index("index_ares_on_account_payment")
                    .query { q ->
                        q.bool { b ->
                            if (request.currencyType != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("currencyType").query(FieldValue.of(request.currencyType))
                                    }
                                }
                            }
                            if (request.entityType != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("entityType").query(FieldValue.of(request.entityType.toString()))
                                    }
                                }
                            }
                            if (request.startDate != null && request.endDate != null) {
                                b.must { m ->
                                    m.range { r ->
                                        r.field("transactionDate")
                                            .gte(JsonData.of(Timestamp.valueOf(request.startDate))).lte(
                                            JsonData.of(Timestamp.valueOf(request.endDate))
                                        )
                                    }
                                }
                            }
                            b
                        }
                    }
                    .from((request.page - 1) * request.pageLimit)
                    .size(request.pageLimit)
            },
            classType
        )
        return response
    }
}
