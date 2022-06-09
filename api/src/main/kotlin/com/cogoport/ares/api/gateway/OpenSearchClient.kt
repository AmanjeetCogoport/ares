package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.brahma.opensearch.Client
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOrder
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
        for (hts in response?.hits()?.hits()!!) {
            outResp = hts.source()
        }
        return outResp
    }

    fun <T : Any> searchList(searchKey: String?, classType: Class<T>, index: String, offset: Int, limit: Int): SearchResponse<T>? {

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

    fun listCustomerSaleOutstanding(index: String, classType: Class<CustomerOutstanding>, values: String): SearchResponse<CustomerOutstanding>? {
        val response = Client.search(
            { s ->
                s.index(index)
                    .query {
                        q ->
                        q.matchPhrase { a -> a.field("organizationId").query(values) }
                    }
            },
            classType
        )
        return response
    }

    fun <T : Any> onAccountSearch(request: AccountCollectionRequest, classType: Class<T>): SearchResponse<T>? {
        val response = Client.search(
            { s ->
                s.index("index_ares_on_account_payment")
                    .query { q ->
                        q.bool { b ->
                            b.must { t ->
                                t.match { v -> v.field("deleted").query(FieldValue.of(false)) }
                            }
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
                            if (request.query != null) {
                                b.must { m ->
                                    m.queryString { q -> q.query("*" + request.query + "*").fields("customerName", "utr") }
                                }
                            }
                            b
                        }
                    }
                    .from((request.page - 1) * request.pageLimit)
                    .size(request.pageLimit)
                    .sort { t ->
                        t.field { f -> f.field("transactionDate").order(SortOrder.Desc) }
                    }
            },
            classType
        )
        return response
    }

    fun orgDetailSearch(orgSerialId: Long): SearchResponse<Any>? {
        val index = "organization_details"
        val searchResponse = Client.search({ s ->
            s.index(index)
                .source { a -> a.filter { f -> f.includes("organizationId") } }
                .query { q ->
                    q.match { m -> m.field("organizationSerialId").query(FieldValue.of(orgSerialId)) }
                }
        }, Any::class.java)
        return searchResponse
    }
}
