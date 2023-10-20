package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequestV2
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponseV2
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocumentV2
import com.cogoport.brahma.opensearch.Client
import io.micronaut.tracing.annotation.NewSpan
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Script
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import java.sql.Timestamp
import java.time.LocalDateTime

class OpenSearchClient {

    @NewSpan
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

    @NewSpan
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

    @NewSpan
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

    @NewSpan
    fun <T> updateDocument(index: String, docId: String, docData: T) {
        Client.updateDocument(index, docId, docData, true)
    }

    @NewSpan
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

    @NewSpan
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

    @NewSpan
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

    @NewSpan
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

    @NewSpan
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

    @NewSpan
    fun listSupplierOutstanding(request: SupplierOutstandingRequest, index: String): SearchResponse<SupplierOutstandingDocument>? {
        val offset = 0.coerceAtLeast(((request.page!! - 1) * request.limit!!))
        val searchFilterFields: MutableList<String> = mutableListOf("businessName", "registrationNumber.keyword")
        val categoryTypes: MutableList<String> = mutableListOf("shipping_line", "airline", "nvocc", "iata", "transporter", "freight_forwarder", "customs_service_provider")
        val response = Client.search({ t ->
            t.index(index)
                .query { q ->
                    q.bool { b ->
                        if (request.q != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(searchFilterFields).query("*${request.q}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.sageId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("sageId.keyword")).query("*${request.sageId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.organizationSerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("organizationSerialId.keyword")).query("*${request.organizationSerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartySerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("serialId.keyword")).query("*${request.tradePartySerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.supplyAgentId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("supplyAgent.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.supplyAgentId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.countryId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("countryId.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.countryId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.companyType != null) {
                            b.must { t ->
                                t.match { v ->
                                    v.field("companyType.keyword").query(FieldValue.of(request.companyType))
                                }
                            }
                            b
                        }
                        if (request.category != null) {
                            if (request.category in categoryTypes) {
                                b.must { t ->
                                    t.match { v ->
                                        v.field("category").query(FieldValue.of(request.category)).operator(Operator.And)
                                    }
                                }
                                b
                            } else {
                                b.mustNot { s ->
                                    s.terms { v ->
                                        v.field("category").terms(
                                            TermsQueryField.of { a ->
                                                a.value(
                                                    categoryTypes.map {
                                                        FieldValue.of(it)
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                                b
                            }
                        }
                        b
                    }
                    q
                }
                .sort { t ->
                    if (!request.sortBy.isNullOrBlank()) {
                        if (!request.sortType.isNullOrBlank()) {
                            t.field { f -> f.field(request.sortBy).order(SortOrder.valueOf(request.sortType.toString())) }
                        } else {
                            t.field { f -> f.field(request.sortBy).order(SortOrder.Desc) }
                        }
                    } else {
                        t.field { f -> f.field("businessName.keyword").order(SortOrder.Asc) }
                    }
                }
                .from(offset).size(request.limit)
        }, SupplierOutstandingDocument::class.java)

        return response
    }

    @NewSpan
    fun listCustomerOutstanding(request: CustomerOutstandingRequest, index: String): SearchResponse<CustomerOutstandingDocumentResponse>? {
        val offset = 0.coerceAtLeast(((request.page!! - 1) * request.limit!!))
        var totalOutstanding = false
        var onAccountPayment = false
        var creditNote = false
        var notDue = false
        var thirty = false
        var fortyFive = false
        var sixty = false
        var ninety = false
        var oneEighty = false
        var threeSixtyFive = false
        var oneEightyPlus = false
        var threeSixtyFivePlus = false
        var callPriority = false

        when (request.sortBy) {
            "totalOutstandingLedgerAmount" -> totalOutstanding = true
            "onAccountPaymentLedgerAmount" -> onAccountPayment = true
            "creditNoteLedgerAmount" -> creditNote = true
            "openInvoiceNotDueLedgerAmount" -> notDue = true
            "openInvoiceThirtyLedgerAmount" -> thirty = true
            "openInvoiceFortyFiveLedgerAmount" -> fortyFive = true
            "openInvoiceSixtyLedgerAmount" -> sixty = true
            "openInvoiceNinetyLedgerAmount" -> ninety = true
            "openInvoiceOneEightyLedgerAmount" -> oneEighty = true
            "openInvoiceThreeSixtyFiveLedgerAmount" -> threeSixtyFive = true
            "openInvoiceOneEightyPlusLedgerAmount" -> oneEightyPlus = true
            "openInvoiceThreeSixtyFivePlusLedgerAmount" -> threeSixtyFivePlus = true
            "callPriority" -> callPriority = true
        }

        val sortList = mutableListOf(
            SortOptions.Builder().field { f ->
                when {
                    callPriority -> f.field("totalCallPriorityScore").order(SortOrder.valueOf(request.sortType.toString()))
                    totalOutstanding -> f.field("totalOutstanding.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    onAccountPayment -> f.field("onAccount.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    creditNote -> f.field("creditNote.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    notDue -> f.field("openInvoiceAgeingBucket.notDue.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    thirty -> f.field("openInvoiceAgeingBucket.thirty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    fortyFive -> f.field("openInvoiceAgeingBucket.fortyFive.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    sixty -> f.field("openInvoiceAgeingBucket.sixty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    ninety -> f.field("openInvoiceAgeingBucket.ninety.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    oneEighty -> f.field("openInvoiceAgeingBucket.oneEighty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFive -> f.field("openInvoiceAgeingBucket.threeSixtyFive.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    oneEightyPlus -> f.field("openInvoiceAgeingBucket.oneEightyPlus.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFivePlus -> f.field("openInvoiceAgeingBucket.threeSixtyFivePlus.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                }
                f
            }.build()
        )

        if (callPriority) {
            sortList.add(SortOptions.Builder().field { f -> f.field("totalOutstanding.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString())) }.build())
        }

        val searchFilterFields: MutableList<String> = mutableListOf("businessName", "registrationNumber.keyword", "tradePartySerialId.keyword", "sageId.keyword", "organizationSerialId.keyword")
        val response = Client.search({ t ->
            t.index(index)
                .query { q ->
                    q.bool { b ->
                        if (request.q != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(searchFilterFields).query("*${request.q}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.sageId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("sageId.keyword")).query("*${request.sageId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartyDetailId != null) {
                            b.must { s ->
                                s.match { v ->
                                    v.field("organizationId.keyword").query(FieldValue.of(request.tradePartyDetailId.toString()))
                                }
                            }
                            b
                        }
                        if (request.organizationSerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("organizationSerialId.keyword")).query("*${request.organizationSerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartySerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("tradePartySerialId.keyword")).query("*${request.tradePartySerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.salesAgentId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("salesAgent.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.salesAgentId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.kamId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("kam.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.kamId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.tradePartyDetailIds != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("organizationId.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.tradePartyDetailIds?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.creditControllerId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("creditController.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.creditControllerId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.countryId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("countryId.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.countryId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.companyType != null) {
                            b.must { t ->
                                t.match { v ->
                                    v.field("companyType.keyword").query(FieldValue.of(request.companyType))
                                }
                            }
                            b
                        }
                        b
                    }
                    q
                }
                .sort(sortList)
                .from(offset).size(request.limit)
        }, CustomerOutstandingDocumentResponse::class.java)

        return response
    }

    @NewSpan
    fun listSupplierOutstandingV2(request: SupplierOutstandingRequestV2, index: String): SearchResponse<SupplierOutstandingDocumentV2>? {
        val offset = 0.coerceAtLeast(((request.page!! - 1) * request.pageLimit!!))
        var totalOutstandingAmount = false
        var openOnAccountAmount = false
        var openCreditNoteAmount = false
        var openInvoiceAmount = false
        var notDue = false
        var today = false
        var thirty = false
        var sixty = false
        var ninety = false
        var oneEighty = false
        var threeSixtyFive = false
        var threeSixtyFivePlus = false

        when (request.sortBy) {
            "notDueOutstanding" -> notDue = true
            "todayOutstanding" -> today = true
            "thirtyOutstanding" -> thirty = true
            "sixtyOutstanding" -> sixty = true
            "ninetyOutstanding" -> ninety = true
            "oneEightyOutstanding" -> oneEighty = true
            "threeSixtyFiveOutstanding" -> threeSixtyFive = true
            "threeSixtyFivePlusOutstanding" -> threeSixtyFivePlus = true
            "totalOutstandingAmount" -> totalOutstandingAmount = true
            "openOnAccountAmount" -> openOnAccountAmount = true
            "openCreditNoteAmount" -> openCreditNoteAmount = true
            "openInvoiceAmount" -> openInvoiceAmount = true
        }

        val sortList = mutableListOf(
            SortOptions.Builder().field { f ->
                when {
                    totalOutstandingAmount -> f.field("totalOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    openOnAccountAmount -> f.field("totalOpenOnAccountAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    openCreditNoteAmount -> f.field("totalCreditNoteAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    openInvoiceAmount -> f.field("totalOpenInvoiceAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    notDue -> f.field("notDueOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    today -> f.field("todayOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    thirty -> f.field("thirtyOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    sixty -> f.field("sixtyOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    ninety -> f.field("ninetyOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    oneEighty -> f.field("oneEightyOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFive -> f.field("threeSixtyFiveOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFivePlus -> f.field("threeSixtyFivePlusOutstanding").order(SortOrder.valueOf(request.sortType.toString()))
                }
                f
            }.build()
        )

        val searchFilterFields: MutableList<String> = mutableListOf("organizationName", "registrationNumber.keyword", "bpr", "tradePartySerialId", "organizationSerialId")
        val response = Client.search({ t ->
            t.index(index)
                .query { q ->
                    q.bool { b ->
                        if (request.q != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(searchFilterFields).query("*${request.q}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.supplyAgentId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("agent.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.supplyAgentId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b.must { s ->
                                s.match { it.field("agent.stakeholderType.keyword").query(FieldValue.of("supply_agent")) }
                            }
                            b
                        }
                        if (request.creditControllerId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("agent.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.creditControllerId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b.must { s ->
                                s.match { it.field("agent.stakeholderType.keyword").query(FieldValue.of("credit_controller")) }
                            }
                            b
                        }
                        if (request.orgIds != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("organizationId.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.orgIds?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.organizationSerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("organizationSerialId.keyword")).query("*${request.organizationSerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartySerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("tradePartySerialId.keyword")).query("*${request.tradePartySerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.countryId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("countryId.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.countryId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.companyType != null) {
                            b.must { t ->
                                t.match { v ->
                                    v.field("companyType").query(FieldValue.of(request.companyType))
                                }
                            }
                            b
                        }
                        if (request.entityCode != null) {
                            b.must { t ->
                                t.match { v ->
                                    v.field("entityCode").query(FieldValue.of(request.entityCode.toString()))
                                }
                            }
                            b
                        }
                        b
                    }
                    q
                }
                .sort(sortList)
                .from(offset).size(request.pageLimit)
        }, SupplierOutstandingDocumentV2::class.java)

        return response
    }

    @NewSpan
    fun listCustomerOutstandingV2(request: CustomerOutstandingRequest): SearchResponse<CustomerOutstandingDocumentResponseV2>? {
        val offset = 0.coerceAtLeast(((request.page!! - 1) * request.limit!!))
        var totalOutstanding = false
        var onAccountPayment = false
        var creditNote = false
        var notDue = false
        var thirty = false
        var sixty = false
        var ninety = false
        var oneEighty = false
        var threeSixtyFive = false
        var threeSixtyFivePlus = false
        var callPriority = false

        when (request.sortBy) {
            "totalOutstanding" -> totalOutstanding = true
            "onAccountAmount" -> onAccountPayment = true
            "creditNoteAmount" -> creditNote = true
            "openInvoiceNotDueLedgerAmount" -> notDue = true
            "openInvoiceThirtyLedgerAmount" -> thirty = true
            "openInvoiceSixtyLedgerAmount" -> sixty = true
            "openInvoiceNinetyLedgerAmount" -> ninety = true
            "openInvoiceOneEightyLedgerAmount" -> oneEighty = true
            "openInvoiceThreeSixtyFiveLedgerAmount" -> threeSixtyFive = true
            "openInvoiceThreeSixtyFivePlusLedgerAmount" -> threeSixtyFivePlus = true
            "callPriority" -> callPriority = true
        }

        val sortList = mutableListOf(
            SortOptions.Builder().field { f ->
                when {
                    callPriority -> f.field("totalCallPriorityScore").order(SortOrder.valueOf(request.sortType.toString()))
                    totalOutstanding -> f.field("totalOutstanding.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    onAccountPayment -> f.field("onAccount.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    creditNote -> f.field("creditNote.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    notDue -> f.field("openInvoiceAgeingBucket.notDue.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    thirty -> f.field("openInvoiceAgeingBucket.thirty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    sixty -> f.field("openInvoiceAgeingBucket.sixty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    ninety -> f.field("openInvoiceAgeingBucket.ninety.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    oneEighty -> f.field("openInvoiceAgeingBucket.oneEighty.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFive -> f.field("openInvoiceAgeingBucket.threeSixtyFive.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                    threeSixtyFivePlus -> f.field("openInvoiceAgeingBucket.threeSixtyFivePlus.ledgerAmount").order(SortOrder.valueOf(request.sortType.toString()))
                }
                f
            }.build()
        )

        if (callPriority) {
            sortList.add(SortOptions.Builder().field { f -> f.field("totalOutstanding").order(SortOrder.valueOf(request.sortType.toString())) }.build())
        }

        val searchFilterFields: MutableList<String> = mutableListOf("businessName", "registrationNumber.keyword", "tradePartySerialId.keyword", "sageId.keyword", "organizationSerialId.keyword")
        val response = Client.search({ t ->
            t.index(AresConstants.CUSTOMER_OUTSTANDING_V2)
                .query { q ->
                    q.bool { b ->
                        if (request.q != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(searchFilterFields).query("*${request.q}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.sageId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("sageId")).query("*${request.sageId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartyDetailId != null) {
                            b.must { s ->
                                s.match { v ->
                                    v.field("organizationId").query(FieldValue.of(request.tradePartyDetailId.toString()))
                                }
                            }
                            b
                        }
                        if (request.organizationSerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("organizationSerialId")).query("*${request.organizationSerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.tradePartySerialId != null) {
                            b.must { s ->
                                s.queryString { qs ->
                                    qs.fields(mutableListOf("tradePartySerialId")).query("*${request.tradePartySerialId}*")
                                        .lenient(true)
                                        .allowLeadingWildcard(true)
                                        .defaultOperator(Operator.And)
                                }
                            }
                            b
                        }
                        if (request.salesAgentId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("salesAgent.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.salesAgentId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.kamId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("kam.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.kamId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.tradePartyDetailIds != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("organizationId").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.tradePartyDetailIds?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.creditControllerId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("creditController.id.keyword").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.creditControllerId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.countryId != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("countryId").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.countryId?.map {
                                                    FieldValue.of(it.toString())
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        if (request.companyType != null) {
                            b.must { t ->
                                t.match { v ->
                                    v.field("companyType").query(FieldValue.of(request.companyType))
                                }
                            }
                            b
                        }
                        if (request.entityCode != null) {
                            b.must { s ->
                                s.terms { v ->
                                    v.field("entityCode").terms(
                                        TermsQueryField.of { a ->
                                            a.value(
                                                request.entityCode!!.split("_").map {
                                                    FieldValue.of(it)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            b
                        }
                        b
                    }
                    q
                }
                .sort(sortList)
                .from(offset).size(request.limit)
        }, CustomerOutstandingDocumentResponseV2::class.java)

        return response
    }
}
