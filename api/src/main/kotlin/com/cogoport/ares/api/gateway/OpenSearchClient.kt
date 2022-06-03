package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.SalesTrend
import com.cogoport.brahma.opensearch.Client
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse

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
                s.index(index).query {
                    q ->
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

    fun salesTrendTotalSales(zone: String?): SearchResponse<SalesTrend>? {
        return Client.search(
            { s ->
                s.index("index_invoices")
                    .query { q ->
                        if (!zone.isNullOrBlank()) {
                            q.matchPhrase { m -> m.field("zone").query(zone) }
                        } else {
                            q.matchAll { s -> s.queryName("") }
                        }
                    }
                    .size(0)
                    .aggregations("total_sales") { a ->
                        a.dateHistogram { d -> d.field("invoiceDate").interval { i -> i.time("month") } }
                            .aggregations("amount") { a ->
                                a.sum { s -> s.field("invoiceAmount") }
                            }
                    }
            }, SalesTrend::class.java
        )
    }

    fun salesTrendCreditSales(zone: String?): SearchResponse<SalesTrend>? {
        return Client.search(
            { s ->
                s.index("index_invoices")
                    .query { q ->
                        q.bool { b ->
                            if (zone.isNullOrBlank()) {
                                b.must { t -> t.range { r -> r.field("creditDays").gt(JsonData.of(0)) } }
                            } else {
                                b.must { t -> t.match { m -> m.field("zone").query(FieldValue.of(zone)) } }
                                b.must { t -> t.range { r -> r.field("creditDays").gt(JsonData.of(0)) } }
                            }
                        }
                    }
                    .size(0)
                    .aggregations("credit_sales") { a ->
                        a.global { g -> g }
                        a.dateHistogram { d -> d.field("invoiceDate").interval { i -> i.time("month") } }
                            .aggregations("amount") { a ->
                                a.sum { s -> s.field("invoiceAmount") }
                            }
                    }
            }, SalesTrend::class.java
        )
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
}
