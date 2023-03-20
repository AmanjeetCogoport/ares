package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.config.OpenSearchConfig
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.CustomerOutstandingList
import com.cogoport.ares.model.payment.CustomersOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.SupplierOutstandingList
import com.cogoport.ares.model.payment.SuppliersOutstanding
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.BillOutStandingAgeingResponse
import com.cogoport.ares.model.payment.response.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.InvoicesOutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.OutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.ceil

@Singleton
class OutStandingServiceImpl : OutStandingService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var outstandingAgeingConverter: OutstandingAgeingMapper

    @Inject
    lateinit var businessPartnersImpl: DefaultedBusinessPartnersServiceImpl

    @Inject
    lateinit var orgOutstandingConverter: OrgOutstandingMapper

    @Inject
    lateinit var openSearchServiceImpl: OpenSearchServiceImpl

    @Inject private lateinit var openSearchConfig: OpenSearchConfig

    private fun validateInput(request: OutstandingListRequest) {
        try {
            request.orgIds.map {
                UUID.fromString(it)
            }
        } catch (exception: IllegalArgumentException) {
            throw AresException(AresError.ERR_1009, AresConstants.ORG_ID + " : " + request.orgId)
        }
        if (AresConstants.ROLE_ZONE_HEAD == request.role && request.zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
    }

    override suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList {
        if (request.orgId != null) {
            request.orgIds.add(request.orgId!!)
        }
        validateInput(request)
        val defaultersOrgIds = businessPartnersImpl.listTradePartyDetailIds()
        val orgIds: MutableList<UUID> = mutableListOf()
        request.orgIds.map {
            orgIds.add(UUID.fromString(it))
        }
        val queryResponse = accountUtilizationRepository.getOutstandingAgeingBucket(request.zone, "%" + request.query + "%", orgIds, request.page, request.pageLimit, defaultersOrgIds, request.flag!!)
        val ageingBucket = mutableListOf<OutstandingAgeingResponse>()
        val orgId = mutableListOf<String>()
        queryResponse.forEach { ageing ->
            val docId = if (request.zone != null) "${ageing.organizationId}_${request.zone}" else "${ageing.organizationId}_ALL"
            orgId.add(docId)
            ageingBucket.add(outstandingAgeingConverter.convertToModel(ageing))
        }
        val response = OpenSearchClient().listApi(index = AresConstants.SALES_OUTSTANDING_INDEX, classType = CustomerOutstanding::class.java, values = orgId, offset = (request.page - 1) * request.pageLimit, limit = request.pageLimit)
        val listOrganization: MutableList<CustomerOutstanding?> = mutableListOf()
        val total = response?.hits()?.total()?.value()!!.toDouble()
        for (hts in response.hits()?.hits()!!) {
            val output: CustomerOutstanding? = hts.source()
            for (ageing in ageingBucket) {
                if (ageing.organizationId == output?.organizationId) {
                    val zero = assignAgeingBucket("Not Due", ageing.notDueAmount, ageing.notDueCount, "not_due")
                    val thirty = assignAgeingBucket("1-30", ageing.thirtyAmount, ageing.thirtyCount, "1_30")
                    val sixty = assignAgeingBucket("31-60", ageing.sixtyAmount, ageing.sixtyCount, "31_60")
                    val ninety = assignAgeingBucket("61-90", ageing.ninetyAmount, ageing.ninetyCount, "61_90")
                    val oneEighty = assignAgeingBucket("91-180", ageing.oneeightyAmount, ageing.oneeightyCount, "91_180")
                    val threeSixtyFive = assignAgeingBucket("180-365", ageing.threesixfiveAmount, ageing.threesixfiveCount, "180_365")
                    val year = assignAgeingBucket("365+", ageing.threesixfiveplusAmount, ageing.threesixfiveplusCount, "365")
                    output?.ageingBucket = listOf(zero, thirty, sixty, ninety, oneEighty, threeSixtyFive, year)
                }
            }
            listOrganization.add(output)
        }

        return OutstandingList(
            list = listOrganization.sortedBy { it?.organizationName?.uppercase() },
            totalPage = ceil(total / request.pageLimit.toDouble()).toInt(),
            totalRecords = total.toInt(),
            page = request.page
        )
    }

    override suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val response = mutableListOf<CustomerInvoiceResponse?>()
        val list: SearchResponse<CustomerInvoiceResponse>? = OpenSearchClient().searchList(
            searchKey = request.orgId,
            classType = CustomerInvoiceResponse::class.java,
            index = AresConstants.INVOICE_OUTSTANDING_INDEX,
            offset = offset,
            limit = request.pageLimit
        )
        list?.hits()?.hits()?.map {
            it.source()?.let { it1 -> response.add(it1) }
        }

        val total = list?.hits()?.total()?.value()!!.toDouble()

        return ListInvoiceResponse(
            list = response,
            page = request.page,
            totalPage = ceil(total / request.pageLimit.toDouble()).toInt(),
            totalRecords = total.toInt()
        )
    }

    private fun assignAgeingBucket(ageDuration: String, amount: BigDecimal?, count: Int, key: String): AgeingBucket {
        return AgeingBucket(
            ageingDuration = ageDuration,
            amount = amount,
            count = count,
            ageingDurationKey = key
        )
    }

    override suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?> {
        val listOrganization: MutableList<CustomerOutstanding?> = mutableListOf()
        val customerOutstanding = OpenSearchClient().listCustomerSaleOutstanding(index = AresConstants.SALES_OUTSTANDING_INDEX, classType = CustomerOutstanding::class.java, values = "${orgId}_ALL")

        customerOutstanding?.hits()?.hits()?.map {
            it.source()?.let {
                it1 ->
                listOrganization.add(it1)
            }
        }
        return listOrganization
    }

    override suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long {
        var outstandingDays = accountUtilizationRepository.getCurrentOutstandingDays(invoiceIds)
        if (outstandingDays < 0) {
            outstandingDays = 0
        }
        return outstandingDays
    }

    override suspend fun getCustomersOutstandingInINR(orgIds: List<String>): MutableMap<String, BigDecimal?> {
        val organizationOutStanding: HashMap<String, BigDecimal?> = hashMapOf()
        var totalOutStanding = BigDecimal.ZERO
        val listOrganization: MutableList<CustomerOutstanding?> = mutableListOf()
        orgIds.forEach {
            val customerOutstanding = OpenSearchClient().listCustomerOutstandingOfAllZone(index = AresConstants.SALES_OUTSTANDING_INDEX, classType = CustomerOutstanding::class.java, values = "${it}_ALL")
            customerOutstanding?.hits()?.hits()?.map {
                it.source()?.let { it1 ->
                    listOrganization.add(it1)
                }
            }
        }
        listOrganization.forEach {
            organizationOutStanding[it?.organizationId!!] = it.totalOutstanding?.amountDue?.first()?.amount!!
        }
        return organizationOutStanding
    }

    override suspend fun getSupplierOutstandingList(request: OutstandingListRequest): SupplierOutstandingList {
        validateInput(request)
        val queryResponse = accountUtilizationRepository.getBillsOutstandingAgeingBucket(request.zone, "%" + request.query + "%", request.orgId, request.entityCode, request.page, request.pageLimit)
        val totalRecords = accountUtilizationRepository.getBillsOutstandingAgeingBucketCount(request.zone, "%" + request.query + "%", request.orgId)
        val ageingBucket = mutableListOf<BillOutStandingAgeingResponse>()
        val listOrganization: MutableList<SuppliersOutstanding?> = mutableListOf()
        val listOrganizationIds: MutableList<String?> = mutableListOf()
        queryResponse.forEach { it ->
            ageingBucket.add(outstandingAgeingConverter.convertToOutstandingModel(it))
            listOrganizationIds.add(it.organizationId)
        }

        ageingBucket.forEach { it ->
            val data = accountUtilizationRepository.generateBillOrgOutstanding(it.organizationId!!, request.zone, request.entityCode)
            val dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
            val invoicesDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.openInvoicesAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            val paymentsDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.paymentsAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.paymentsCount!! }) }.toMutableList()
            val outstandingDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.outstandingAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            val invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
            val paymentsCount = dataModel.sumOf { it.paymentsCount!! }
            val invoicesLedgerAmount = dataModel.sumOf { it.openInvoicesLedAmount?.abs()!! }
            val paymentsLedgerAmount = dataModel.sumOf { it.paymentsLedAmount?.abs()!! }
            val outstandingLedgerAmount = dataModel.sumOf { it.outstandingLedAmount?.abs()!! }
            openSearchServiceImpl.validateDueAmount(invoicesDues)
            openSearchServiceImpl.validateDueAmount(paymentsDues)
            openSearchServiceImpl.validateDueAmount(outstandingDues)
            val orgId = it.organizationId
            val orgName = it.organizationName
            val orgOutstanding = SuppliersOutstanding(orgId, orgName, request.zone, InvoiceStats(invoicesCount, invoicesLedgerAmount, invoicesDues.sortedBy { it.currency }), InvoiceStats(paymentsCount, paymentsLedgerAmount, paymentsDues.sortedBy { it.currency }), InvoiceStats(invoicesCount, outstandingLedgerAmount, outstandingDues.sortedBy { it.currency }), null, it.creditNoteCount, it.totalCreditAmount)
            val zero = assignAgeingBucket("Not Due", it.notDueAmount?.abs(), it.notDueCount, "not_due")
            val today = assignAgeingBucket("Today", it.todayAmount?.abs(), it.todayCount, "today")
            val thirty = assignAgeingBucket("1-30", it.thirtyAmount?.abs(), it.thirtyCount, "1_30")
            val sixty = assignAgeingBucket("31-60", it.sixtyAmount?.abs(), it.sixtyCount, "31_60")
            val ninety = assignAgeingBucket("61-90", it.ninetyAmount?.abs(), it.ninetyCount, "61_90")
            val oneEighty = assignAgeingBucket("91-180", it.oneeightyAmount?.abs(), it.oneeightyCount, "91_180")
            val threeSixtyFive = assignAgeingBucket("181-365", it.threesixtyfiveAmount?.abs(), it.threesixtyfiveCount, "181_365")
            val threeSixtyFivePlus = assignAgeingBucket("365+", it.threesixtyfiveplusAmount?.abs(), it.threesixtyfiveplusCount, "365")
            orgOutstanding.ageingBucket = listOf(zero, today, thirty, sixty, ninety, oneEighty, threeSixtyFive, threeSixtyFivePlus)
            listOrganization.add(orgOutstanding)
        }

        return SupplierOutstandingList(
            list = listOrganization.sortedBy { it?.organizationName?.uppercase() },
            totalPage = ceil(totalRecords / request.pageLimit.toDouble()).toInt(),
            totalRecords = totalRecords,
            page = request.page
        )
    }

    override suspend fun updateSupplierDetails(id: String, flag: Boolean, document: SupplierOutstandingDocument?) {
        logger().info("Starting to update supplier details of $id")
        configureOpenSearchForRabbitMqListener()
        try {
            var supplierOutstanding: SupplierOutstandingDocument? = null
            if (flag) {
                supplierOutstanding = document
            } else {
                val searchResponse = Client.search({ s ->
                    s.index(AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX)
                        .query { q ->
                            q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(id)) }
                        }
                }, SupplierOutstandingDocument::class.java)
                if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
                    supplierOutstanding = searchResponse?.hits()?.hits()?.map { it.source() }?.get(0)
                }
            }

            if (supplierOutstanding != null) {
                var outstandingResponse: SupplierOutstandingDocument

                val overallOutstanding = getSupplierOutstandingList(OutstandingListRequest(orgId = id))
                if (!overallOutstanding.list.isNullOrEmpty()) {
                    outstandingResponse = supplierOutstandingResponseMapper(overallOutstanding, supplierOutstanding)
                    Client.updateDocument(AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX, id, outstandingResponse, true)
                }

                AresConstants.COGO_ENTITIES.forEach {
                    val outstandingForEntity = getSupplierOutstandingList(OutstandingListRequest(orgId = id, entityCode = it))
                    if (!outstandingForEntity.list.isNullOrEmpty()) {
                        outstandingResponse = supplierOutstandingResponseMapper(outstandingForEntity, supplierOutstanding)
                        val index = "supplier_outstanding_$it"
                        Client.updateDocument(index, id, outstandingResponse, true)
                    }
                }
            }
        } catch (error: Exception) {
            logger().error(error.toString())
            logger().error(error.stackTraceToString())
        }
    }

    /**
     * Workaround for making rabbitmq consumers interact with
     * opensearch client since on deployments, consumers start
     * before the server startup event.
     */
    private fun configureOpenSearchForRabbitMqListener() {
        if (Client.getLowLevelClient() == null) {
            Client.configure(
                configuration =
                Configuration(
                    scheme = openSearchConfig.scheme,
                    host = openSearchConfig.host,
                    port = openSearchConfig.port,
                    user = openSearchConfig.user,
                    pass = openSearchConfig.pass
                )
            )
        }
    }

    override suspend fun createSupplierDetails(request: SupplierOutstandingDocument) {
        val searchResponse = Client.search({ s ->
            s.index(AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX)
                .query { q ->
                    q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(request.organizationId)) }
                }
        }, SupplierOutstandingDocument::class.java)

        if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
            updateSupplierDetails(request.organizationId!!, flag = true, request)
        } else {

            val supplierOutstandingDocument = outstandingAgeingConverter.convertSupplierDetailsRequestToDocument(request)
            supplierOutstandingDocument.updatedAt = Timestamp.valueOf(LocalDateTime.now())
            supplierOutstandingDocument.onAccountPayment = listOf<DueAmount>()
            supplierOutstandingDocument.totalOutstanding = listOf<DueAmount>()
            supplierOutstandingDocument.openInvoice = listOf<DueAmount>()
            supplierOutstandingDocument.onAccountPaymentInvoiceCount = 0
            supplierOutstandingDocument.openInvoiceCount = 0
            supplierOutstandingDocument.totalOutstandingInvoiceCount = 0
            supplierOutstandingDocument.totalOutstandingInvoiceLedgerAmount = BigDecimal.ZERO
            supplierOutstandingDocument.onAccountPaymentInvoiceLedgerAmount = BigDecimal.ZERO
            supplierOutstandingDocument.openInvoiceLedgerAmount = BigDecimal.ZERO
            supplierOutstandingDocument.totalCreditNoteAmount = BigDecimal.ZERO
            supplierOutstandingDocument.creditNoteCount = 0
            supplierOutstandingDocument.notDueAmount = BigDecimal.ZERO
            supplierOutstandingDocument.notDueCount = 0
            supplierOutstandingDocument.todayAmount = BigDecimal.ZERO
            supplierOutstandingDocument.todayCount = 0
            supplierOutstandingDocument.thirtyAmount = BigDecimal.ZERO
            supplierOutstandingDocument.thirtyCount = 0
            supplierOutstandingDocument.sixtyAmount = BigDecimal.ZERO
            supplierOutstandingDocument.sixtyCount = 0
            supplierOutstandingDocument.ninetyAmount = BigDecimal.ZERO
            supplierOutstandingDocument.ninetyCount = 0
            supplierOutstandingDocument.oneEightyAmount = BigDecimal.ZERO
            supplierOutstandingDocument.oneEightyCount = 0
            supplierOutstandingDocument.threeSixtyFiveAmount = BigDecimal.ZERO
            supplierOutstandingDocument.threeSixtyFiveCount = 0
            supplierOutstandingDocument.threeSixtyFivePlusAmount = BigDecimal.ZERO
            supplierOutstandingDocument.threeSixtyFivePlusCount = 0
            Client.addDocument(AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX, request.organizationId!!, supplierOutstandingDocument, true)
            AresConstants.COGO_ENTITIES.forEach {
                val index = "supplier_outstanding_$it"
                Client.addDocument(index, request.organizationId!!, supplierOutstandingDocument, true)
            }
        }
    }

    override suspend fun listSupplierDetails(request: SupplierOutstandingRequest): ResponseList<SupplierOutstandingDocument?> {
        var index: String = AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX

        if (request.flag != "overall") {
            index = "supplier_outstanding_${request.flag}"
        }

        val response = OpenSearchClient().listSupplierOutstanding(request, index)
        var list: List<SupplierOutstandingDocument?> = listOf()
        if (!response?.hits()?.hits().isNullOrEmpty()) {
            list = response?.hits()?.hits()?.map { it.source() }!!
        }
        val responseList = ResponseList<SupplierOutstandingDocument?>()

        responseList.list = list
        responseList.totalRecords = response?.hits()?.total()?.value() ?: 0
        responseList.totalPages = if (responseList.totalRecords!! % request.limit!! == 0.toLong()) (responseList.totalRecords!! / request.limit!!) else (responseList.totalRecords!! / request.limit!!) + 1.toLong()
        responseList.pageNo = request.page!!

        return responseList
    }

    private fun supplierOutstandingResponseMapper(outstanding: SupplierOutstandingList, supplierOutstanding: SupplierOutstandingDocument): SupplierOutstandingDocument {
        var supplierOutstandingDocument: SupplierOutstandingDocument? = null

        outstanding.list!!.forEach { supplier ->
            supplierOutstandingDocument = SupplierOutstandingDocument(
                organizationId = supplierOutstanding.organizationId,
                selfOrganizationId = supplierOutstanding.selfOrganizationId,
                businessName = supplierOutstanding.businessName,
                selfOrganizationName = supplierOutstanding.selfOrganizationName,
                registrationNumber = supplierOutstanding.registrationNumber,
                serialId = supplierOutstanding.serialId,
                organizationSerialId = supplierOutstanding.organizationSerialId,
                sageId = supplierOutstanding.sageId,
                countryCode = supplierOutstanding.countryCode,
                countryId = supplierOutstanding.countryId,
                category = supplierOutstanding.category,
                collectionPartyType = supplierOutstanding.collectionPartyType,
                companyType = supplierOutstanding.companyType,
                supplyAgent = supplierOutstanding.supplyAgent,
                creditDays = supplierOutstanding.creditDays,
                updatedAt = Timestamp.valueOf(LocalDateTime.now()),
                onAccountPayment = supplier?.onAccountPayment!!.amountDue,
                totalOutstanding = supplier.totalOutstanding!!.amountDue,
                openInvoice = supplier.openInvoices!!.amountDue,
                onAccountPaymentInvoiceCount = supplier.onAccountPayment!!.invoicesCount,
                openInvoiceCount = supplier.openInvoices!!.invoicesCount,
                totalOutstandingInvoiceCount = supplier.totalOutstanding!!.invoicesCount,
                totalOutstandingInvoiceLedgerAmount = supplier.totalOutstanding!!.invoiceLedAmount,
                onAccountPaymentInvoiceLedgerAmount = supplier.onAccountPayment!!.invoiceLedAmount,
                openInvoiceLedgerAmount = supplier.openInvoices!!.invoiceLedAmount,
                totalCreditNoteAmount = supplier.totalCreditAmount,
                creditNoteCount = supplier.creditNoteCount,
                notDueAmount = supplier.ageingBucket?.filter { it.ageingDuration == "Not Due" }?.get(0)?.amount,
                notDueCount = supplier.ageingBucket?.filter { it.ageingDuration == "Not Due" }?.get(0)?.count,
                todayAmount = supplier.ageingBucket?.filter { it.ageingDuration == "Today" }?.get(0)?.amount,
                todayCount = supplier.ageingBucket?.filter { it.ageingDuration == "Today" }?.get(0)?.count,
                thirtyAmount = supplier.ageingBucket?.filter { it.ageingDuration == "1-30" }?.get(0)?.amount,
                thirtyCount = supplier.ageingBucket?.filter { it.ageingDuration == "1-30" }?.get(0)?.count,
                sixtyAmount = supplier.ageingBucket?.filter { it.ageingDuration == "31-60" }?.get(0)?.amount,
                sixtyCount = supplier.ageingBucket?.filter { it.ageingDuration == "31-60" }?.get(0)?.count,
                ninetyAmount = supplier.ageingBucket?.filter { it.ageingDuration == "61-90" }?.get(0)?.amount,
                ninetyCount = supplier.ageingBucket?.filter { it.ageingDuration == "61-90" }?.get(0)?.count,
                oneEightyAmount = supplier.ageingBucket?.filter { it.ageingDuration == "91-180" }?.get(0)?.amount,
                oneEightyCount = supplier.ageingBucket?.filter { it.ageingDuration == "91-180" }?.get(0)?.count,
                threeSixtyFiveAmount = supplier.ageingBucket?.filter { it.ageingDuration == "181-365" }?.get(0)?.amount,
                threeSixtyFiveCount = supplier.ageingBucket?.filter { it.ageingDuration == "181-365" }?.get(0)?.count,
                threeSixtyFivePlusAmount = supplier.ageingBucket?.filter { it.ageingDuration == "365+" }?.get(0)?.amount,
                threeSixtyFivePlusCount = supplier.ageingBucket?.filter { it.ageingDuration == "365+" }?.get(0)?.count
            )
        }
        return supplierOutstandingDocument!!
    }

    override suspend fun createCustomerDetails(request: CustomerOutstandingDocumentResponse) {
        val searchResponse = Client.search({ s ->
            s.index(AresConstants.CUSTOMERS_OUTSTANDING_OVERALL_INDEX)
                .query { q ->
                    q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(request.organizationId)) }
                }
        }, CustomerOutstandingDocumentResponse::class.java)

        if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
            updateCustomerDetails(request.organizationId!!, flag = true, request)
        } else {

            val customerOutstandingDocument = outstandingAgeingConverter.convertCustomerDetailsRequestToDocument(request)
            customerOutstandingDocument.updatedAt = Timestamp.valueOf(LocalDateTime.now())
            customerOutstandingDocument.onAccountPayment = listOf<DueAmount>()
            customerOutstandingDocument.totalOutstanding = listOf<DueAmount>()
            customerOutstandingDocument.openInvoice = listOf<DueAmount>()
            customerOutstandingDocument.onAccountPaymentInvoiceCount = 0
            customerOutstandingDocument.openInvoiceCount = 0
            customerOutstandingDocument.totalOutstandingInvoiceCount = 0
            customerOutstandingDocument.totalOutstandingInvoiceLedgerAmount = BigDecimal.ZERO
            customerOutstandingDocument.onAccountPaymentInvoiceLedgerAmount = BigDecimal.ZERO
            customerOutstandingDocument.openInvoiceLedgerAmount = BigDecimal.ZERO
            customerOutstandingDocument.totalCreditNoteAmount = BigDecimal.ZERO
            customerOutstandingDocument.totalDebitNoteAmount = BigDecimal.ZERO
            customerOutstandingDocument.creditNoteCount = 0
            customerOutstandingDocument.debitNoteCount = 0
            customerOutstandingDocument.notDueAmount = BigDecimal.ZERO
            customerOutstandingDocument.notDueCount = 0
            customerOutstandingDocument.todayAmount = BigDecimal.ZERO
            customerOutstandingDocument.todayCount = 0
            customerOutstandingDocument.thirtyAmount = BigDecimal.ZERO
            customerOutstandingDocument.thirtyCount = 0
            customerOutstandingDocument.sixtyAmount = BigDecimal.ZERO
            customerOutstandingDocument.sixtyCount = 0
            customerOutstandingDocument.ninetyAmount = BigDecimal.ZERO
            customerOutstandingDocument.ninetyCount = 0
            customerOutstandingDocument.oneEightyAmount = BigDecimal.ZERO
            customerOutstandingDocument.oneEightyCount = 0
            customerOutstandingDocument.threeSixtyFiveAmount = BigDecimal.ZERO
            customerOutstandingDocument.threeSixtyFiveCount = 0
            customerOutstandingDocument.threeSixtyFivePlusAmount = BigDecimal.ZERO
            customerOutstandingDocument.threeSixtyFivePlusCount = 0
            Client.addDocument(AresConstants.SUPPLIERS_OUTSTANDING_OVERALL_INDEX, request.organizationId!!, customerOutstandingDocument, true)
            AresConstants.COGO_ENTITIES.forEach {
                val index = "customer_outstanding_$it"
                Client.addDocument(index, request.organizationId!!, customerOutstandingDocument, true)
            }
        }
    }

    override suspend fun updateCustomerDetails(id: String, flag: Boolean, document: CustomerOutstandingDocumentResponse?) {
        logger().info("Starting to update customer details of $id")
        configureOpenSearchForRabbitMqListener()
        try {
            var customerOutstanding: CustomerOutstandingDocumentResponse? = null
            if (flag) {
                customerOutstanding = document
            } else {
                val searchResponse = Client.search({ s ->
                    s.index(AresConstants.CUSTOMERS_OUTSTANDING_OVERALL_INDEX)
                        .query { q ->
                            q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(id)) }
                        }
                }, CustomerOutstandingDocumentResponse::class.java)
                if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
                    customerOutstanding = searchResponse?.hits()?.hits()?.map { it.source() }?.get(0)
                }
            }

            if (customerOutstanding != null) {
                var outstandingResponse: CustomerOutstandingDocumentResponse

                val overallOutstanding = getCustomerOutstandingList(OutstandingListRequest(orgId = id))
                if (!overallOutstanding.list.isNullOrEmpty()) {
                    outstandingResponse = customerOutstandingResponseMapper(overallOutstanding, customerOutstanding)
                    Client.updateDocument(AresConstants.CUSTOMERS_OUTSTANDING_OVERALL_INDEX, id, outstandingResponse, true)
                }

                AresConstants.COGO_ENTITIES.forEach {
                    val outstandingForEntity = getCustomerOutstandingList(OutstandingListRequest(orgId = id, entityCode = it))
                    if (!outstandingForEntity.list.isNullOrEmpty()) {
                        outstandingResponse = customerOutstandingResponseMapper(outstandingForEntity, customerOutstanding)
                        val index = "customer_outstanding_$it"
                        Client.updateDocument(index, id, outstandingResponse, true)
                    }
                }
            }
        } catch (error: Exception) {
            logger().error(error.toString())
            logger().error(error.stackTraceToString())
        }
    }

    override suspend fun getCustomerOutstandingList(request: OutstandingListRequest): CustomerOutstandingList {
        validateInput(request)
        val queryResponse = accountUtilizationRepository.getInvoicesOutstandingAgeingBucket(request.zone, "%" + request.query + "%", request.orgId, request.entityCode, request.page, request.pageLimit)
        val totalRecords = accountUtilizationRepository.getInvoicesOutstandingAgeingBucketCount(request.zone, "%" + request.query + "%", request.orgId)
        val ageingBucket = mutableListOf<InvoicesOutstandingAgeingResponse>()
        val listOrganization: MutableList<CustomersOutstanding?> = mutableListOf()
        val listOrganizationIds: MutableList<String?> = mutableListOf()
        queryResponse.forEach { it ->
            ageingBucket.add(outstandingAgeingConverter.convertToInvoiceOutstandingModel(it))
            listOrganizationIds.add(it.organizationId)
        }

        ageingBucket.forEach { it ->
            val data = accountUtilizationRepository.generateOrgOutstanding(it.organizationId!!, request.zone, request.entityCode)
            val dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
            val invoicesDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.openInvoicesAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            val paymentsDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.paymentsAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.paymentsCount!! }) }.toMutableList()
            val outstandingDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.outstandingAmount?.abs().toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            val invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
            val paymentsCount = dataModel.sumOf { it.paymentsCount!! }
            val invoicesLedgerAmount = dataModel.sumOf { it.openInvoicesLedAmount?.abs()!! }
            val paymentsLedgerAmount = dataModel.sumOf { it.paymentsLedAmount?.abs()!! }
            val outstandingLedgerAmount = dataModel.sumOf { it.outstandingLedAmount?.abs()!! }
            openSearchServiceImpl.validateDueAmount(invoicesDues)
            openSearchServiceImpl.validateDueAmount(paymentsDues)
            openSearchServiceImpl.validateDueAmount(outstandingDues)
            val orgId = it.organizationId
            val orgName = it.organizationName
            val orgOutstanding = CustomersOutstanding(orgId, orgName, request.zone, InvoiceStats(invoicesCount, invoicesLedgerAmount, invoicesDues.sortedBy { it.currency }), InvoiceStats(paymentsCount, paymentsLedgerAmount, paymentsDues.sortedBy { it.currency }), InvoiceStats(invoicesCount, outstandingLedgerAmount, outstandingDues.sortedBy { it.currency }), null, it.creditNoteCount, it.totalCreditAmount, it.debitNoteCount, it.totalDebitAmount)
            val zero = assignAgeingBucket("Not Due", it.notDueAmount?.abs(), it.notDueCount, "not_due")
            val today = assignAgeingBucket("Today", it.todayAmount?.abs(), it.todayCount, "today")
            val thirty = assignAgeingBucket("1-30", it.thirtyAmount?.abs(), it.thirtyCount, "1_30")
            val sixty = assignAgeingBucket("31-60", it.sixtyAmount?.abs(), it.sixtyCount, "31_60")
            val ninety = assignAgeingBucket("61-90", it.ninetyAmount?.abs(), it.ninetyCount, "61_90")
            val oneEighty = assignAgeingBucket("91-180", it.oneEightyAmount?.abs(), it.oneEightyCount, "91_180")
            val threeSixtyFive = assignAgeingBucket("181-365", it.threeSixtyFiveAmount?.abs(), it.threeSixtyFiveCount, "181_365")
            val threeSixtyFivePlus = assignAgeingBucket("365+", it.threeSixtyFivePlusAmount?.abs(), it.threeSixtyFivePlusCount, "365")
            orgOutstanding.ageingBucket = listOf(zero, today, thirty, sixty, ninety, oneEighty, threeSixtyFive, threeSixtyFivePlus)
            listOrganization.add(orgOutstanding)
        }

        return CustomerOutstandingList(
            list = listOrganization.sortedBy { it?.organizationName?.uppercase() },
            totalPage = ceil(totalRecords / request.pageLimit.toDouble()).toInt(),
            totalRecords = totalRecords,
            page = request.page
        )
    }

    override suspend fun listCustomerDetails(request: CustomerOutstandingRequest): ResponseList<CustomerOutstandingDocumentResponse?> {
        var index: String = AresConstants.CUSTOMERS_OUTSTANDING_OVERALL_INDEX

        if (request.flag != "overall") {
            index = "customer_outstanding_${request.flag}"
        }

        val response = OpenSearchClient().listCustomerOutstanding(request, index)
        var list: List<CustomerOutstandingDocumentResponse?> = listOf()
        if (!response?.hits()?.hits().isNullOrEmpty()) {
            list = response?.hits()?.hits()?.map { it.source() }!!
        }
        val responseList = ResponseList<CustomerOutstandingDocumentResponse?>()

        responseList.list = list
        responseList.totalRecords = response?.hits()?.total()?.value() ?: 0
        responseList.totalPages = if (responseList.totalRecords!! % request.limit!! == 0.toLong()) (responseList.totalRecords!! / request.limit!!) else (responseList.totalRecords!! / request.limit!!) + 1.toLong()
        responseList.pageNo = request.page!!

        return responseList
    }

    private fun customerOutstandingResponseMapper(outstanding: CustomerOutstandingList, customerOutstanding: CustomerOutstandingDocumentResponse): CustomerOutstandingDocumentResponse {
        var customerOutstandingDocument: CustomerOutstandingDocumentResponse? = null

        outstanding.list!!.forEach { customer ->
            customerOutstandingDocument = CustomerOutstandingDocumentResponse(
                organizationId = customerOutstanding.organizationId,
                businessName = customerOutstanding.businessName,
                legalBusinessName = customerOutstanding.legalBusinessName,
                tradePartyName = customerOutstanding.tradePartyName,
                tradePartyId = customerOutstanding.tradePartyId,
                tradePartyType = customerOutstanding.tradePartyType,
                registrationNumber = customerOutstanding.registrationNumber,
                tradePartySerialId = customerOutstanding.tradePartySerialId,
                organizationSerialId = customerOutstanding.organizationSerialId,
                sageId = customerOutstanding.sageId,
                countryCode = customerOutstanding.countryCode,
                countryId = customerOutstanding.countryId,
                companyType = customerOutstanding.companyType,
                creditController = customerOutstanding.creditController,
                kam = customerOutstanding.kam,
                salesAgent = customerOutstanding.salesAgent,
                creditDays = customerOutstanding.creditDays,
                updatedAt = Timestamp.valueOf(LocalDateTime.now()),
                onAccountPayment = customer?.onAccountPayment!!.amountDue,
                totalOutstanding = customer.totalOutstanding!!.amountDue,
                openInvoice = customer.openInvoices!!.amountDue,
                onAccountPaymentInvoiceCount = customer.onAccountPayment!!.invoicesCount,
                openInvoiceCount = customer.openInvoices!!.invoicesCount,
                totalOutstandingInvoiceCount = customer.totalOutstanding!!.invoicesCount,
                totalOutstandingInvoiceLedgerAmount = customer.totalOutstanding!!.invoiceLedAmount,
                onAccountPaymentInvoiceLedgerAmount = customer.onAccountPayment!!.invoiceLedAmount,
                openInvoiceLedgerAmount = customer.openInvoices!!.invoiceLedAmount,
                totalCreditNoteAmount = customer.totalCreditAmount,
                totalDebitNoteAmount = customer.totalDebitAmount,
                creditNoteCount = customer.creditNoteCount,
                debitNoteCount = customer.debitNoteCount,
                notDueAmount = customer.ageingBucket?.filter { it.ageingDuration == "Not Due" }?.get(0)?.amount,
                notDueCount = customer.ageingBucket?.filter { it.ageingDuration == "Not Due" }?.get(0)?.count,
                todayAmount = customer.ageingBucket?.filter { it.ageingDuration == "Today" }?.get(0)?.amount,
                todayCount = customer.ageingBucket?.filter { it.ageingDuration == "Today" }?.get(0)?.count,
                thirtyAmount = customer.ageingBucket?.filter { it.ageingDuration == "1-30" }?.get(0)?.amount,
                thirtyCount = customer.ageingBucket?.filter { it.ageingDuration == "1-30" }?.get(0)?.count,
                sixtyAmount = customer.ageingBucket?.filter { it.ageingDuration == "31-60" }?.get(0)?.amount,
                sixtyCount = customer.ageingBucket?.filter { it.ageingDuration == "31-60" }?.get(0)?.count,
                ninetyAmount = customer.ageingBucket?.filter { it.ageingDuration == "61-90" }?.get(0)?.amount,
                ninetyCount = customer.ageingBucket?.filter { it.ageingDuration == "61-90" }?.get(0)?.count,
                oneEightyAmount = customer.ageingBucket?.filter { it.ageingDuration == "91-180" }?.get(0)?.amount,
                oneEightyCount = customer.ageingBucket?.filter { it.ageingDuration == "91-180" }?.get(0)?.count,
                threeSixtyFiveAmount = customer.ageingBucket?.filter { it.ageingDuration == "181-365" }?.get(0)?.amount,
                threeSixtyFiveCount = customer.ageingBucket?.filter { it.ageingDuration == "181-365" }?.get(0)?.count,
                threeSixtyFivePlusAmount = customer.ageingBucket?.filter { it.ageingDuration == "365+" }?.get(0)?.amount,
                threeSixtyFivePlusCount = customer.ageingBucket?.filter { it.ageingDuration == "365+" }?.get(0)?.count
            )
        }
        return customerOutstandingDocument!!
    }
}
