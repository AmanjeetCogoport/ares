package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.LedgerSummaryRequest
import com.cogoport.ares.model.payment.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.brahma.opensearch.Client
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldSort
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Script
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse

class OpenSearchClient {

    fun <T : Any> search(
        searchKey: String,
        classType: Class<T>,
        index: String = AresConstants.SALES_DASHBOARD_INDEX
    ): T? {

        val response =
            Client.search(
                { s: SearchRequest.Builder ->
                    s.index(index).query { q: Query.Builder ->
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

    fun <T : Any> searchList(
        searchKey: String?,
        classType: Class<T>,
        index: String,
        offset: Int,
        limit: Int
    ): SearchResponse<T>? {

        val response: SearchResponse<T>? =
            Client.search(
                { s ->
                    s.index(index)
                        .query { q ->
                            if (!searchKey.isNullOrBlank()) {
                                q.matchPhrase { a ->
                                    a.field("organizationId").query(searchKey)
                                }
                            } else {
                                q.matchAll { s -> s.queryName("") }
                            }
                            q
                        }
                        .from(offset)
                        .size(limit)
                },
                classType
            )
        return response
    }

    fun <T : Any> listApi(
        index: String,
        classType: Class<T>,
        values: List<String>,
        offset: Int? = null,
        limit: Int? = null
    ): SearchResponse<T>? {
        val response =
            Client.search(
                { s ->
                    s.index(index)
                        .query { q -> q.ids { i -> i.values(values) } }
                        .from(offset)
                        .size(limit)
                },
                classType
            )
        return response
    }

    fun <T> updateDocument(index: String, docId: String, docData: T) {
        Client.updateDocument(index, docId, docData)
    }

    fun listCustomerSaleOutstanding(
        index: String,
        classType: Class<CustomerOutstanding>,
        values: String
    ): SearchResponse<CustomerOutstanding>? {
        val response =
            Client.search(
                { s ->
                    s.index(index).query { q ->
                        q.matchPhrase { a -> a.field("organizationId").query(values) }
                    }
                },
                classType
            )
        return response
    }

    fun <T : Any> onAccountSearch(
        request: AccountCollectionRequest,
        classType: Class<T>
    ): SearchResponse<T>? {
        val response =
            Client.search(
                { s ->
                    s.index("index_ares_on_account_payment")
                        .query { q ->
                            q.bool { b ->
                                b.must { t ->
                                    t.match { v ->
                                        v.field("isDeleted").query(FieldValue.of(false))
                                    }
                                }
                                if (request.currencyType != null) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("currency")
                                                .query(
                                                    FieldValue.of(
                                                        request.currencyType
                                                    )
                                                )
                                        }
                                    }
                                }
                                if (request.entityType != null) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("entityType")
                                                .query(
                                                    FieldValue.of(
                                                        request.entityType
                                                            .toString()
                                                    )
                                                )
                                        }
                                    }
                                }
                                if (request.startDate != null && request.endDate != null
                                ) {
                                    b.must { m ->
                                        m.range { r ->
                                            r.field("transactionDate")
                                                .gte(
                                                    JsonData.of(
                                                        Timestamp.valueOf(
                                                            request.startDate
                                                        )
                                                    )
                                                )
                                                .lte(
                                                    JsonData.of(
                                                        Timestamp.valueOf(
                                                            request.endDate
                                                        )
                                                    )
                                                )
                                        }
                                    }
                                }
                                if (request.query != null) {
                                    b.must { m ->
                                        m.queryString { q ->
                                            q.query("*" + request.query + "*")
                                                .fields("organizationName", "utr")
                                        }
                                    }
                                }
                                b
                            }
                        }
                        .from((request.page - 1) * request.pageLimit)
                        .size(request.pageLimit)
                        .sort { t ->
                            t.field { f -> f.field("id").order(SortOrder.Desc) }
                        }
                },
                classType
            )
        return response
    }

    fun getOrgCollection(
        request: OrganizationReceivablesRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<AccountUtilizationResponse?>? {
        return Client.search(
            { s ->
                s.index("index_account_utilization").size(10000).query { q ->
                    q.bool { b ->
                        b.must {
                            it.match { it.field("accMode").query(FieldValue.of("AP")) }
                        }
                        b.mustNot {
                            it.match {
                                it.field("documentStatus")
                                    .query(FieldValue.of("CANCELLED"))
                            }
                        }
                        b.mustNot {
                            it.match {
                                it.field("documentStatus")
                                    .query(FieldValue.of("DELETED"))
                            }
                        }
                        b.must { m ->
                            m.range { r ->
                                r.field("transactionDate")
                                    .gte(JsonData.of(Timestamp.valueOf(startDate)))
                            }
                        }
                        b.must { m ->
                            m.range { r ->
                                r.field("transactionDate")
                                    .lt(JsonData.of(Timestamp.valueOf(endDate)))
                            }
                        }
                        if (request.orgId != null) {
                            b.must { m ->
                                m.match {
                                    it.field("organizationId")
                                        .query(
                                            FieldValue.of(
                                                request.orgId.toString()
                                            )
                                        )
                                }
                            }
                        }
                        b
                    }
                }
            },
            AccountUtilizationResponse::class.java
        )
            ?.hits()
            ?.hits()
            ?.map { it.source() }
    }

    fun getOrgPayables(
        orgId: String? = null,
        startDate: Timestamp? = null,
        endDate: Timestamp? = null
    ): SearchResponse<Void>? {
        return Client.search(
            { s ->
                s.index(AresConstants.ACCOUNT_UTILIZATION_INDEX)
                    .query { q ->
                        q.bool { b ->
                            b.must { m ->
                                m.match { f ->
                                    f.field("accMode").query(FieldValue.of("AP"))
                                }
                            }
                            b.mustNot {
                                it.match {
                                    it.field("documentStatus")
                                        .query(FieldValue.of("CANCELLED"))
                                }
                            }
                            b.mustNot {
                                it.match {
                                    it.field("documentStatus")
                                        .query(FieldValue.of("DELETED"))
                                }
                            }
                            if (orgId != null) {
                                b.must { m ->
                                    m.match { f ->
                                        f.field("organizationId")
                                            .query(FieldValue.of(orgId))
                                    }
                                }
                            }
                            if (startDate != null) {
                                b.must { m ->
                                    m.range { r ->
                                        r.field("dueDate").gte(JsonData.of(startDate))
                                    }
                                }
                            }
                            if (endDate != null) {
                                b.must { m ->
                                    m.range { r ->
                                        r.field("dueDate").lt(JsonData.of(endDate))
                                    }
                                }
                            }
                            b
                        }
                    }
                    .size(0)
                    .aggregations("currency") { a ->
                        a.terms { t -> t.field("currency.keyword") }.aggregations(
                            "currAmount"
                        ) { a ->
                            a.sum { s ->
                                s.script(
                                    Script.of { i ->
                                        i.inline {
                                            it.source(
                                                "doc['signFlag'].value * (doc['amountCurr'].value - doc['payCurr'].value)"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    .aggregations("ledgerAmount") { a ->
                        a.sum { s ->
                            s.script(
                                Script.of { i ->
                                    i.inline {
                                        it.source(
                                            "doc['signFlag'].value * (doc['amountLoc'].value - doc['payLoc'].value)"
                                        )
                                    }
                                }
                            )
                        }
                    }
            },
            Void::class.java
        )
    }

    fun <T : Any> onAccountUtilizationSearch(
        request: LedgerSummaryRequest,
        classType: Class<T>
    ): SearchResponse<T>? {
        val response =
            Client.search(
                { s ->
                    s.index(AresConstants.ACCOUNT_UTILIZATION_INDEX)
                        .query { q ->
                            q.bool { b ->
                                b.must { m ->
                                    m.match { m ->
                                        m.field(AresConstants.ACCMODE)
                                            .query(
                                                FieldValue.of(
                                                    AresConstants.MODE
                                                )
                                            )
                                    }
                                }
                                b.must { m ->
                                    m.match { m ->
                                        m.field(AresConstants.ORGANIZATION_ID)
                                            .query(FieldValue.of(request.orgId))
                                    }
                                }
                                if (request.startDate != null && request.endDate != null
                                ) {
                                    b.must { m ->
                                        m.range { r ->
                                            r.field(AresConstants.TRANSACTION_DATE)
                                                .lte(
                                                    JsonData.of(
                                                        Timestamp.valueOf(
                                                            request.endDate
                                                                .toString()
                                                        )
                                                    )
                                                )
                                        }
                                    }
                                }
                                b
                            }
                        }
                        .size(1000)
                        .sort { t ->
                            t.field { f -> f.field("id").order(SortOrder.Asc) }
                        }
                },
                classType
            )
        return response
    }

    fun getSettlementInvoices(
        request: SettlementDocumentRequest
    ): SearchResponse<AccountUtilizationResponse>? {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        return Client.search(
            { s ->
                s.index(AresConstants.ACCOUNT_UTILIZATION_INDEX)
                    .query { q ->
                        q.bool { b ->
                            if (request.entityCode != null) {
                                b.must { m ->
                                    m.match {
                                        it.field("entityCode")
                                            .query(
                                                FieldValue.of(
                                                    request.entityCode
                                                        .toString()
                                                )
                                            )
                                    }
                                }
                            }
                            if (request.orgId != null) {
                                b.must { m ->
                                    m.match {
                                        it.field("organizationId")
                                            .query(
                                                FieldValue.of(
                                                    request.orgId.toString()
                                                )
                                            )
                                    }
                                }
                            }
                            if (request.startDate != null) {
                                b.must { m ->
                                    m.range {
                                        it.field("transactionDate")
                                            .gte(
                                                JsonData.of(
                                                    Timestamp.valueOf(
                                                        request.startDate
                                                    )
                                                )
                                            )
                                    }
                                }
                            }
                            if (request.endDate != null) {
                                b.must { m ->
                                    m.range {
                                        it.field("transactionDate")
                                            .lte(
                                                JsonData.of(
                                                    Timestamp.valueOf(
                                                        request.endDate
                                                    )
                                                )
                                            )
                                    }
                                }
                            }
                            if (!request.query.isNullOrBlank()) {
                                b.must { m ->
                                    m.queryString { q ->
                                        q.query("*" + escapeSlash(request.query!!) + "*")
                                            .fields("documentValue.keyword")
                                    }
                                }
                            }
                            if (request.accMode != null) {
                                b.must { m ->
                                    m.match {
                                        it.field("accMode")
                                            .query(
                                                FieldValue.of(
                                                    request.accMode.toString()
                                                )
                                            )
                                    }
                                }
                            }
                            b.must { m ->
                                m.match {
                                    it.field("documentStatus").query(FieldValue.of("FINAL"))
                                }
                            }
                        }
                    }
                    .size(request.pageLimit)
                    .from(offset)
                    .sort { s ->
                        s.field(FieldSort.of { it.field("id").order(SortOrder.Desc) })
                    }
            },
            AccountUtilizationResponse::class.java
        )
    }

    private fun escapeSlash(query: String): String {
        val test = query.replace("/", "\\/").uppercase()
        return test
    }

    fun getSummary(request: SummaryRequest? = null, documentIds: List<String>? = null): BigDecimal {
        return Client.search(
            { s ->
                s.index(AresConstants.ACCOUNT_UTILIZATION_INDEX)
                    .query { q ->
                        q.bool { b ->
                            b.must { m ->
                                m.match {
                                    it.field("documentStatus")
                                        .query(FieldValue.of("FINAL"))
                                }
                            }
                            if (documentIds != null) {
                                b.must { m ->
                                    m.ids { it.values(documentIds) }
                                }
                            } else {
                                if (request?.orgId != null) {
                                    b.minimumShouldMatch("1")
                                    request.orgId!!.forEach { id ->
                                        b.should { m ->
                                            m.match {
                                                it.field("organizationId")
                                                    .query(
                                                        FieldValue
                                                            .of(
                                                                id
                                                            )
                                                    )
                                            }
                                        }
                                    }
                                }
                                if (request?.entityCode != null)
                                    b.must { m ->
                                        m.match {
                                            it.field("entityCode")
                                                .query(
                                                    FieldValue
                                                        .of(
                                                            request.entityCode
                                                        )
                                                )
                                        }
                                    }
                                if (request?.startDate != null) {
                                    b.must { m ->
                                        m.range {
                                            it.field("transactionDate")
                                                .gte(
                                                    JsonData.of(
                                                        Timestamp
                                                            .valueOf(
                                                                request.startDate
                                                            )
                                                    )
                                                )
                                        }
                                    }
                                }
                                if (request?.endDate != null) {
                                    b.must { m ->
                                        m.range {
                                            it.field("transactionDate")
                                                .lte(
                                                    JsonData.of(
                                                        Timestamp
                                                            .valueOf(
                                                                request.endDate
                                                            )
                                                    )
                                                )
                                        }
                                    }
                                }
                            }
                            b
                        }
                    }
                    .aggregations("outstandingAmount") { a ->
                        a.sum { s ->
                            s.script { sc ->
                                sc.inline {
                                    it.source(
                                        "doc['signFlag'].value * (doc['amountLoc'].value - doc['payLoc'].value)"
                                    )
                                }
                            }
                        }
                    }
            },
            Void::class.java
        )
            ?.aggregations()
            ?.get("outstandingAmount")
            ?.sum()
            ?.value()!!
            .toBigDecimal()
    }
}
