package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.brahma.opensearch.Client
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Script
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import java.sql.Timestamp
import java.time.LocalDateTime

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
        Client.updateDocument(index, docId, docData, true)
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
                        q.matchPhrase { a -> a.field("_id").query(values) }
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
                    s.index(AresConstants.ON_ACCOUNT_PAYMENT_INDEX)
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
                                            q.query("*${request.query!!}*")
                                                .fields("organizationName", "utr")
                                                .lenient(true)
                                                .escape(true)
                                                .allowLeadingWildcard(true)
                                                .defaultOperator(Operator.And)
                                        }
                                    }
                                }
                                b
                            }
                        }
                        .from((request.page - 1) * request.pageLimit)
                        .size(request.pageLimit)
                        .sort { t ->
                            t.field { f -> f.field("createdAt").order(SortOrder.Desc) }
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
                s.index(AresConstants.ACCOUNT_UTILIZATION_INDEX).size(10000).query { q ->
                    q.bool { b ->
                        b.must {
                            it.match { it.field("accMode").query(FieldValue.of("AP")) }
                        }
                        b.must { m ->
                            m.script { s ->
                                s.script { s1 ->
                                    s1.inline { it.source("(doc['amountCurr'].value - doc['payCurr'].value) != 0") }
                                }
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
                        ) { b ->
                            b.sum { s ->
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
                                    m.match { n ->
                                        n.field(AresConstants.ACCMODE)
                                            .query(
                                                FieldValue.of(
                                                    AresConstants.MODE
                                                )
                                            )
                                    }
                                }
                                b.must { m ->
                                    m.match { n ->
                                        n.field("organizationId.keyword")
                                            .query(
                                                FieldValue.of(
                                                    request.orgId
                                                )
                                            )
                                    }
                                }
                                if (request.startDate != null && request.endDate != null) {
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
                        .size(AresConstants.LIMIT)
                        .sort { t ->
                            t.field { f -> f.field("id").order(SortOrder.Asc) }
                        }
                },
                classType
            )
        return response
    }

    fun listCustomerOutstandingOfAllZone(
        index: String,
        classType: Class<CustomerOutstanding>,
        values: String
    ): SearchResponse<CustomerOutstanding>? {
        val response =
            Client.search(
                { s ->
                    s.index(index).query { q ->
                        q.bool { b ->
                            b.must { n ->
                                n.match { v ->
                                    v.field("_id").query(FieldValue.of(values))
                                }
                            }
                            b
                        }
                    }
                },
                classType
            )
        return response
    }


    fun listSupplierOutstanding(request: SupplierOutstandingRequest) : SearchResponse<SupplierOutstandingResponse>? {
        val offset = 0.coerceAtLeast(((request.page!! - 1) * request.limit!!))
        val searchFilterFields: MutableList<String> = mutableListOf("legalName", "businessName")
        val response = Client.search({
            t ->
            t.index(AresConstants.SUPPLIERS_OUTSTANDING_INDEX)
                    .query { q ->
                        q.bool { b ->
                            if (request.name != null) {
                                b.must { s ->
                                    s.queryString { qs ->
                                        qs.fields(searchFilterFields).query("*${request.name}*")
                                                .lenient(true)
                                                .allowLeadingWildcard(true)
                                                .defaultOperator(Operator.Or)
                                    }
                                }
                                b
                            }
                            if (request.supplyAgentIds != null) {
                                b.must { s ->
                                    s.terms { v ->
                                        v.field("supplyAgentId.keyword").terms(
                                                TermsQueryField.of { a ->
                                                    a.value(
                                                            request.supplyAgentIds?.map {
                                                                FieldValue.of(it.toString())
                                                            }
                                                    )
                                                }
                                        )
                                    }
                                }
                                b
                            }
                            if (request.countryIds != null) {
                                b.must { s ->
                                    s.terms { v ->
                                        v.field("countryId.keyword").terms(
                                                TermsQueryField.of { a ->
                                                    a.value(
                                                            request.countryIds?.map {
                                                                FieldValue.of(it.toString())
                                                            }
                                                    )
                                                }
                                        )
                                    }
                                }
                                b
                            }
                            if (request.cogoEntityId != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("cogoEntityId.keyword").query(FieldValue.of(request.cogoEntityId.toString()))
                                    }
                                }
                                b
                            }
                            if (request.taxNo != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("taxNumber.keyword").query(FieldValue.of(request.taxNo.toString()))
                                    }
                                }
                                b
                            }
                            if (request.companyType != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("companyType.keyword").query(FieldValue.of(request.companyType)
                                    }
                                }
                                b
                            }
                            if (request.category != null) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("category.keyword").query(FieldValue.of(request.category)
                                    }
                                }
                                b
                            }
                            if (request.ageingKey.isNotEmpty()) {
                                b.minimumShouldMatch("1")
                                if (request.ageingKey.contains(AresConstants.AGEING_NOT_DUE)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_DUE_TODAY)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_1_30)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_31_60)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_61_90)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_91_180)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_181_365)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                                if (request.ageingKey.contains(AresConstants.AGEING_365_PLUS)) {
                                    b.must { t ->
                                        t.match { v ->
                                            v.field("ageingBucket.ageingDurationKey.keyword").query(FieldValue.of(request.ageingKey)
                                        }
                                    }
                                }
                            b
                        }
                        b
                    }q
                    }.sort {t ->
                        if (request.sortBy != null) {
                            if (request.sortType != null) {
                                if(request.sortBy == "total_outstanding") {
                                    t.field { f -> f.field("totalOutstanding.amountDue.amount").order(SortOrder.valueOf(request.sortType.toString())) }
                                }
                                if(request.sortBy == "on_account") {
                                    t.field { f -> f.field("onAccountPayment.amountDue.amount").order(SortOrder.valueOf(request.sortType.toString())) }
                                }
                                if(request.sortBy == "open_invoices") {
                                    t.field { f -> f.field("openInvoices.amountDue.amount").order(SortOrder.valueOf(request.sortType.toString())) }
                                }
                            }
                            else {
                                if(request.sortBy == "total_outstanding") {
                                    t.field { f -> f.field("totalOutstanding.amountDue.amount").order(SortOrder.Desc)}
                                }
                                if(request.sortBy == "on_account") {
                                    t.field { f -> f.field("onAccountPayment.amountDue.amount").order(SortOrder.Desc) }
                                }
                                if(request.sortBy == "open_invoices") {
                                    t.field { f -> f.field("openInvoices.amountDue.amount").order(SortOrder.Desc) }
                                }
                            }

                        } else {
                            t.field { f -> f.field("businessName").order(SortOrder.Asc) }
                        }
                    }
                    .from(offset).size(request.limit)
                 },SupplierOutstandingResponse::class.java)

        return response
    }
}
