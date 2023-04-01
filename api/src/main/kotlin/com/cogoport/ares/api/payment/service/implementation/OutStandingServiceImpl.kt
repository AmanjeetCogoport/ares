package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.config.OpenSearchConfig
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.CustomerOrgOutstanding
import com.cogoport.ares.api.payment.entity.CustomerOutstandingAgeing
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentRequest
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.payment.model.response.TopTenVendorsRes
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.AgeingBucketOutstanding
import com.cogoport.ares.model.payment.CustomerOutstanding
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
import com.cogoport.ares.model.payment.response.OutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.PayableStatsOpenSearchResponse
import com.cogoport.ares.model.payment.response.PayblesInfoRes
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.sentry.Sentry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.math.ceil

@Singleton
class OutStandingServiceImpl : OutStandingService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

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
        if (AresConstants.ROLE_ZONE_HEAD == request.role) {
            throw AresException(AresError.ERR_1003, "")
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
            it.source()?.let { it1 ->
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
        AresConstants.COGO_ENTITIES.forEach { entity ->
            val index = "customer_outstanding_$entity"
            val searchResponse = Client.search({ s ->
                s.index(index)
                    .query { q ->
                        q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(request.organizationId)) }
                    }
            }, CustomerOutstandingDocumentResponse::class.java)

            if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
                updateCustomerDetails(request.organizationId!!, flag = true, searchResponse?.hits()?.hits()?.map { it.source() }?.get(0))
            } else {
                val queryResponse = accountUtilizationRepo.getInvoicesOutstandingAgeingBucket(entity, AccountType.SINV, request.organizationId)
                val creditNoteQueryResponse = accountUtilizationRepo.getInvoicesOutstandingAgeingBucket(entity, AccountType.SCN, request.organizationId)
                val onAccountRecQueryResponse = accountUtilizationRepo.getInvoicesOnAccountAgeingBucket(entity, request.organizationId)
                if (queryResponse.isNullOrEmpty()) {
                    return@forEach
                }
                val openInvoiceAgeingBucket = getAgeingBucketForCustomerOutstanding(queryResponse, entity)
                val creditNoteAgeingBucket = getAgeingBucketForCustomerOutstanding(creditNoteQueryResponse, entity)
                val onAccountRecAgeingBucket = getAgeingBucketForCustomerOutstanding(onAccountRecQueryResponse, entity)
                val customerOutstanding: CustomerOutstandingDocumentResponse?

                val orgOutstandingData = accountUtilizationRepository.generateCustomerOutstanding(request.organizationId!!, entity)
                val onAccountPayment = getOnAccountPaymentDetails(orgOutstandingData, entity)
                val openInvoice = getOpenInvoiceDetails(orgOutstandingData, entity)
                val totalOutstanding = getTotalOutstandingDetails(orgOutstandingData, entity)
                val creditNote = getCreditNoteDetails(orgOutstandingData, entity)

                customerOutstanding = CustomerOutstandingDocumentResponse(
                    lastUpdatedAt = Timestamp.valueOf(LocalDateTime.now()),
                    organizationId = request.organizationId,
                    tradePartyId = request.tradePartyId,
                    businessName = request.businessName,
                    companyType = request.companyType,
                    openInvoiceAgeingBucket = openInvoiceAgeingBucket,
                    onAccountAgeingBucket = onAccountRecAgeingBucket,
                    creditNoteAgeingBucket = creditNoteAgeingBucket,
                    countryCode = request.countryCode,
                    countryId = request.countryId,
                    creditController = request.creditController,
                    creditDays = request.creditDays,
                    kam = request.kam,
                    organizationSerialId = request.organizationSerialId,
                    registrationNumber = request.registrationNumber,
                    sageId = request.sageId,
                    salesAgent = request.salesAgent,
                    tradePartyName = request.tradePartyName,
                    tradePartySerialId = request.tradePartySerialId,
                    tradePartyType = request.tradePartyType,
                    onAccount = onAccountPayment,
                    openInvoice = openInvoice,
                    creditNote = creditNote,
                    totalOutstanding = totalOutstanding,
                    openInvoiceCount = orgOutstandingData.sumOf { it.openInvoicesCount },
                    onAccountCount = orgOutstandingData.sumOf { it.paymentsCount },
                    entityCode = entity
                )
                Client.addDocument("customer_outstanding_$entity", request.organizationId!!, customerOutstanding, true)
            }
        }
    }

    private fun getOnAccountPaymentDetails(orgOutstandingData: List<CustomerOrgOutstanding>, entity: Int): AgeingBucketOutstanding {
        val onAccountBucket: AgeingBucketOutstanding?
        var onAccountLedAmount = 0.toBigDecimal()
        var onAccountLedCount = 0
        val onAccountInvoiceBucket = mutableListOf<DueAmount>()
        orgOutstandingData.forEach {
            onAccountLedAmount += it.paymentsLedAmount * 1.0.toBigDecimal()
            onAccountLedCount += it.paymentsCount
            if (it.paymentsAmount != 0.toBigDecimal()) {
                onAccountInvoiceBucket.add(DueAmount(it.currency, it.paymentsAmount, it.paymentsCount))
            }
        }
        onAccountBucket = AgeingBucketOutstanding(onAccountLedAmount, onAccountLedCount, AresConstants.LEDGER_CURRENCY[entity]!!, onAccountInvoiceBucket)

        return onAccountBucket
    }

    private fun getOpenInvoiceDetails(orgOutstandingData: List<CustomerOrgOutstanding>, entity: Int): AgeingBucketOutstanding {
        val openInvoiceAgeingBucket: AgeingBucketOutstanding?
        var openInvoiceLedAmount = 0.toBigDecimal()
        var openInvoiceLedCount = 0
        val openInvoiceBucket = mutableListOf<DueAmount>()
        orgOutstandingData.forEach {
            openInvoiceLedAmount += it.openInvoicesLedAmount * 1.0.toBigDecimal()
            openInvoiceLedCount += it.openInvoicesCount
            if (it.openInvoicesAmount != 0.toBigDecimal()) {
                openInvoiceBucket.add(DueAmount(it.currency, it.openInvoicesAmount, it.openInvoicesCount))
            }
        }
        openInvoiceAgeingBucket = AgeingBucketOutstanding(openInvoiceLedAmount, openInvoiceLedCount, AresConstants.LEDGER_CURRENCY[entity]!!, openInvoiceBucket)

        return openInvoiceAgeingBucket
    }

    private fun getTotalOutstandingDetails(orgOutstandingData: List<CustomerOrgOutstanding>, entity: Int): AgeingBucketOutstanding {
        val totalOutstandingBucket: AgeingBucketOutstanding?
        var totalOutstandingLedAmount = 0.toBigDecimal()
        var totalOutstandingLedCount = 0
        val totalOutstandingInvoiceBucket = mutableListOf<DueAmount>()
        orgOutstandingData.forEach {
            totalOutstandingLedAmount += it.outstandingLedAmount * 1.0.toBigDecimal()
            totalOutstandingLedCount += 0
            if (it.outstandingAmount != 0.toBigDecimal()) {
                totalOutstandingInvoiceBucket.add(DueAmount(it.currency, it.outstandingAmount, 0))
            }
        }
        totalOutstandingBucket = AgeingBucketOutstanding(totalOutstandingLedAmount, totalOutstandingLedCount, AresConstants.LEDGER_CURRENCY[entity]!!, totalOutstandingInvoiceBucket)

        return totalOutstandingBucket
    }

    private fun getCreditNoteDetails(orgOutstandingData: List<CustomerOrgOutstanding>, entity: Int): AgeingBucketOutstanding {
        val creditNoteBucket: AgeingBucketOutstanding?
        var creditNoteLedAmount = 0.toBigDecimal()
        var creditNoteLedCount = 0
        val creditNoteInvoiceBucket = mutableListOf<DueAmount>()
        orgOutstandingData.forEach {
            creditNoteLedAmount += it.creditNoteLedAmount * 1.0.toBigDecimal()
            creditNoteLedCount += it.creditNoteCount
            if (it.creditNoteAmount != 0.toBigDecimal()) {
                creditNoteInvoiceBucket.add(DueAmount(it.currency, it.creditNoteAmount, it.creditNoteCount))
            }
        }
        creditNoteBucket = AgeingBucketOutstanding(creditNoteLedAmount, creditNoteLedCount, AresConstants.LEDGER_CURRENCY[entity]!!, creditNoteInvoiceBucket)

        return creditNoteBucket
    }

    private fun getAgeingBucketForCustomerOutstanding(customerOutstanding: List<CustomerOutstandingAgeing>, entity: Int): HashMap<String, AgeingBucketOutstanding> {
        val ageingBucketsInInvoiceCurrency = HashMap<String, AgeingBucketOutstanding>()
        var invoiceCount = 0
        customerOutstanding.forEach {
            invoiceCount += it.notDueCount + it.thirtyCount + it.fortyFiveCount + it.sixtyCount + it.ninetyCount + it.oneEightyCount + it.oneEightyPlusCount
            val notDue = DueAmount(it.currency, it.notDueCurrAmount, it.notDueCount)
            val thirty = DueAmount(it.currency, it.thirtyCurrAmount, it.thirtyCount)
            val fortyFive = DueAmount(it.currency, it.fortyFiveCurrAmount, it.fortyFiveCount)
            val sixty = DueAmount(it.currency, it.sixtyCurrAmount, it.sixtyCount)
            val ninety = DueAmount(it.currency, it.ninetyCurrAmount, it.ninetyCount)
            val oneEighty = DueAmount(it.currency, it.oneEightyCurrAmount, it.oneEightyCount)
            val oneEightyPlus = DueAmount(it.currency, it.oneEightyPlusCurrAmount, it.oneEightyPlusCount)

            if (ageingBucketsInInvoiceCurrency.contains("notDue")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["notDue"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(notDue.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.notDueLedAmount)
                if (notDue.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(notDue)
                }
                ageingBucketsInInvoiceCurrency["notDue"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["notDue"] = AgeingBucketOutstanding(it.notDueLedAmount, it.notDueCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (notDue.amount != 0.toBigDecimal()) mutableListOf(notDue) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("thirty")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["thirty"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(thirty.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.thirtyLedAmount)
                if (thirty.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(thirty)
                }
                ageingBucketsInInvoiceCurrency["thirty"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["thirty"] = AgeingBucketOutstanding(it.thirtyLedAmount, it.thirtyCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (thirty.amount != 0.toBigDecimal()) mutableListOf(thirty) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("fortyFive")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["fortyFive"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(fortyFive.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.fortyFiveLedAmount)
                if (fortyFive.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(fortyFive)
                }
                ageingBucketsInInvoiceCurrency["fortyFive"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["fortyFive"] = AgeingBucketOutstanding(it.fortyFiveLedAmount, it.fortyFiveCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (fortyFive.amount != 0.toBigDecimal()) mutableListOf(fortyFive) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("sixty")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["sixty"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(sixty.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.sixtyLedAmount)
                if (sixty.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(sixty)
                }
                ageingBucketsInInvoiceCurrency["sixty"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["sixty"] = AgeingBucketOutstanding(it.sixtyLedAmount, it.sixtyCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (sixty.amount != 0.toBigDecimal()) mutableListOf(sixty) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("ninety")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["ninety"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(ninety.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.ninetyLedAmount)
                if (ninety.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(ninety)
                }
                ageingBucketsInInvoiceCurrency["ninety"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["ninety"] = AgeingBucketOutstanding(it.ninetyLedAmount, it.ninetyCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (ninety.amount != 0.toBigDecimal()) mutableListOf(ninety) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("oneEighty")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["oneEighty"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(oneEighty.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.oneEightyLedAmount)
                if (oneEighty.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(oneEighty)
                }
                ageingBucketsInInvoiceCurrency["oneEighty"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["oneEighty"] = AgeingBucketOutstanding(it.oneEightyLedAmount, it.oneEightyCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (oneEighty.amount != 0.toBigDecimal()) mutableListOf(oneEighty) else mutableListOf())
            }

            if (ageingBucketsInInvoiceCurrency.contains("oneEightyPlus")) {
                val ageingBucket = ageingBucketsInInvoiceCurrency["oneEightyPlus"]
                ageingBucket?.ledgerCount = ageingBucket?.ledgerCount?.plus(oneEightyPlus.invoicesCount)!!
                ageingBucket.ledgerAmount = ageingBucket.ledgerAmount.plus(it.oneEightyPlusLedAmount)
                if (oneEightyPlus.amount != 0.toBigDecimal()) {
                    ageingBucket.invoiceBucket.add(oneEightyPlus)
                }
                ageingBucketsInInvoiceCurrency["oneEightyPlus"] = ageingBucket
            } else {
                ageingBucketsInInvoiceCurrency["oneEightyPlus"] = AgeingBucketOutstanding(it.oneEightyPlusLedAmount, it.oneEightyPlusCount, AresConstants.LEDGER_CURRENCY[entity]!!, if (oneEightyPlus.amount != 0.toBigDecimal()) mutableListOf(oneEightyPlus) else mutableListOf())
            }
        }

        return ageingBucketsInInvoiceCurrency
    }

    override suspend fun updateCustomerDetails(id: String, flag: Boolean, document: CustomerOutstandingDocumentResponse?) {
        logger().info("Starting to update customer details of $id")
        try {
            var customerOutstanding: CustomerOutstandingDocumentResponse? = null
            if (flag) {
                customerOutstanding = document
            } else {
                AresConstants.COGO_ENTITIES.forEach { entity ->
                    val index = "customer_outstanding_$entity"
                    val searchResponse = Client.search({ s ->
                        s.index(index)
                            .query { q ->
                                q.match { m -> m.field("organizationId.keyword").query(FieldValue.of(id)) }
                            }
                    }, CustomerOutstandingDocumentResponse::class.java)
                    if (!searchResponse?.hits()?.hits().isNullOrEmpty()) {
                        customerOutstanding = searchResponse?.hits()?.hits()?.map { it.source() }?.get(0)
                    }

                    if (customerOutstanding != null) {
                        val queryResponse = accountUtilizationRepo.getInvoicesOutstandingAgeingBucket(entity, AccountType.SINV, id)
                        val creditNoteQueryResponse = accountUtilizationRepo.getInvoicesOutstandingAgeingBucket(entity, AccountType.SCN, id)
                        val onAccountRecQueryResponse = accountUtilizationRepo.getInvoicesOnAccountAgeingBucket(entity, id)
                        if (queryResponse.isNullOrEmpty()) {
                            return@forEach
                        }
                        val openInvoiceAgeingBucket = getAgeingBucketForCustomerOutstanding(queryResponse, entity)
                        val creditNoteAgeingBucket = getAgeingBucketForCustomerOutstanding(creditNoteQueryResponse, entity)
                        val onAccountRecAgeingBucket = getAgeingBucketForCustomerOutstanding(onAccountRecQueryResponse, entity)

                        val orgOutstandingData = accountUtilizationRepository.generateCustomerOutstanding(id, entity)
                        val onAccountPayment = getOnAccountPaymentDetails(orgOutstandingData, entity)
                        val openInvoice = getOpenInvoiceDetails(orgOutstandingData, entity)
                        val totalOutstanding = getTotalOutstandingDetails(orgOutstandingData, entity)
                        val creditNote = getCreditNoteDetails(orgOutstandingData, entity)

                        val openSearchData = CustomerOutstandingDocumentResponse(
                            lastUpdatedAt = Timestamp.valueOf(LocalDateTime.now()),
                            organizationId = customerOutstanding?.organizationId ?: id,
                            tradePartyId = customerOutstanding?.tradePartyId,
                            businessName = customerOutstanding?.businessName,
                            companyType = customerOutstanding?.companyType,
                            openInvoiceAgeingBucket = openInvoiceAgeingBucket,
                            onAccountAgeingBucket = onAccountRecAgeingBucket,
                            creditNoteAgeingBucket = creditNoteAgeingBucket,
                            countryCode = customerOutstanding?.countryCode,
                            countryId = customerOutstanding?.countryId,
                            creditController = customerOutstanding?.creditController,
                            creditDays = customerOutstanding?.creditDays,
                            kam = customerOutstanding?.kam,
                            organizationSerialId = customerOutstanding?.organizationSerialId,
                            registrationNumber = customerOutstanding?.registrationNumber,
                            sageId = customerOutstanding?.sageId,
                            salesAgent = customerOutstanding?.salesAgent,
                            tradePartyName = customerOutstanding?.tradePartyName,
                            tradePartySerialId = customerOutstanding?.tradePartySerialId,
                            tradePartyType = customerOutstanding?.tradePartyType,
                            onAccount = onAccountPayment,
                            openInvoice = openInvoice,
                            creditNote = creditNote,
                            totalOutstanding = totalOutstanding,
                            openInvoiceCount = orgOutstandingData.sumOf { it.openInvoicesCount },
                            onAccountCount = orgOutstandingData.sumOf { it.paymentsCount },
                            entityCode = entity
                        )
                        Client.addDocument("customer_outstanding_$entity", id, openSearchData, true)
                    }
                }
            }
        } catch (error: Exception) {
            logger().error(error.stackTraceToString())
            Sentry.captureException(error)
        }
    }

    override suspend fun listCustomerDetails(request: CustomerOutstandingRequest): ResponseList<CustomerOutstandingDocumentResponse?> {
        val index = "customer_outstanding_${request.entityCode}"

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

    override suspend fun getCustomerOutstandingPaymentDetails(request: CustomerOutstandingPaymentRequest): ResponseList<CustomerOutstandingPaymentResponse?> {

        val list: List<CustomerOutstandingPaymentResponse?>
        list = accountUtilizationRepo.getPaymentByTradePartyMappingId(request.orgId!!, request.sortBy, request.sortType, request.statusList, "%${request.query}%", request.page, request.pageLimit)

        val responseList = ResponseList<CustomerOutstandingPaymentResponse?>()

        responseList.list = list
        responseList.totalRecords = list.size.toLong()
        responseList.totalPages = if (responseList.totalRecords!! % request.pageLimit == 0.toLong()) (responseList.totalRecords!! / request.pageLimit) else (responseList.totalRecords!! / request.pageLimit) + 1.toLong()
        responseList.pageNo = request.page

        return responseList
    }

    override suspend fun getPayablesInfo(entity: Int?): PayblesInfoRes {
        if (entity == null) {
            throw AresException(AresError.ERR_1003, "Entity not found")
        }
        val payblesInfo = PayblesInfoRes()
        payblesInfo.accountPayables = accountUtilizationRepository.getAccountPayables(entity)?.multiply(BigDecimal(-1))
        val accountPayablesStats = accountUtilizationRepository.getAccountPayablesStats(entity)
        payblesInfo.openInvoicesCount = accountPayablesStats.openInvoiceCount
        payblesInfo.openInvoicesAmount = accountPayablesStats.openInvoiceAmount
        payblesInfo.onAccountAmount = accountPayablesStats.onAccountAmount
        payblesInfo.creditNoteAmount = accountPayablesStats.creditNoteAmount
        payblesInfo.organizationsCount = accountUtilizationRepo.getOrganizationCount(entity)
        payblesInfo.openInvoiceChange = getPaybleChange("openInvoice", entity)
        payblesInfo.onAccountChange = getPaybleChange("onAccount", entity)
        payblesInfo.creditNoteChange = getPaybleChange("creditNote", entity)
        payblesInfo.currency = AresConstants.LEDGER_CURRENCY.get(entity)
        return payblesInfo
    }

    private fun getPaybleChange(change: String, entity: Int?): BigDecimal {
        val previousDateStart = Utilities.localDateTimeToTimeStamp(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIN))
        val previousDateEnd = Utilities.localDateTimeToTimeStamp(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX))
        val weekBeforeStart = Utilities.localDateTimeToTimeStamp(LocalDateTime.of(LocalDate.now().minusDays(8), LocalTime.MIN))
        val weekBeforeEnd = Utilities.localDateTimeToTimeStamp(LocalDateTime.of(LocalDate.now().minusDays(8), LocalTime.MAX))
        var previousDay: SearchResponse<PayableStatsOpenSearchResponse>?
        var weekBefore: SearchResponse<PayableStatsOpenSearchResponse>?
        try {
            previousDay = Client.search({ s ->
                s.index(AresConstants.PAYABLES_STATS_INDEX)
                    .query { q ->
                        q.bool { b ->
                            b.must { m ->
                                m.range { r ->
                                    r.field("date.keyword")
                                        .gte(JsonData.of(previousDateStart.toString()))
                                        .lte(JsonData.of(previousDateEnd.toString()))
                                }
                            }
                            if (entity != null) {
                                b.must {
                                    it.match {
                                        it.field("entity")
                                            .query(FieldValue.of(entity.toString()))
                                    }
                                }
                            }
                            b
                        }
                    }
                    .aggregations("sumOpenInvoice") { a ->
                        a.sum { s ->
                            s.field("openInvoiceAmount")
                        }
                    }
                    .aggregations("sumOnAccount") { a ->
                        a.sum { s ->
                            s.field("onAccountAmount")
                        }
                    }
                    .aggregations("sumCreditNote") { a ->
                        a.sum { s ->
                            s.field("creditNoteAmount")
                        }
                    }
            }, PayableStatsOpenSearchResponse::class.java)

            weekBefore = Client.search({ s ->
                s.index(AresConstants.PAYABLES_STATS_INDEX)
                    .query { q ->
                        q.bool { b ->
                            b.must { m ->
                                m.range { r ->
                                    r.field("date.keyword")
                                        .gte(JsonData.of(weekBeforeStart.toString()))
                                        .lte(JsonData.of(weekBeforeEnd.toString()))
                                }
                            }
                            if (entity != null) {
                                b.must {
                                    it.match {
                                        it.field("entity")
                                            .query(FieldValue.of(entity.toString()))
                                    }
                                }
                            }
                            b
                        }
                    }
                    .aggregations("sumOpenInvoice") { a ->
                        a.sum { s ->
                            s.field("openInvoiceAmount")
                        }
                    }
                    .aggregations("sumOnAccount") { a ->
                        a.sum { s ->
                            s.field("onAccountAmount")
                        }
                    }
                    .aggregations("sumCreditNote") { a ->
                        a.sum { s ->
                            s.field("creditNoteAmount")
                        }
                    }
            }, PayableStatsOpenSearchResponse::class.java)
        } catch (e: Exception) {
            return BigDecimal.ZERO
        }

        var changeValue: BigDecimal = BigDecimal.ZERO

        val previousDayOpenInvoice = previousDay?.aggregations()?.get("sumOpenInvoice")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO
        val previousDayOnAccount = previousDay?.aggregations()?.get("sumOnAccount")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO
        val previousDayCreditNote = previousDay?.aggregations()?.get("sumCreditNote")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO

        val weekBeforeOpenInvoice = weekBefore?.aggregations()?.get("sumOpenInvoice")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO
        val weekBeforeOnAccount = weekBefore?.aggregations()?.get("sumOnAccount")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO
        val weekBeforeCreditNote = weekBefore?.aggregations()?.get("sumCreditNote")?.sum()?.value()?.toBigDecimal() ?: BigDecimal.ZERO

        if (change == "openInvoice") {
            if (previousDayOpenInvoice.compareTo(BigDecimal.ZERO) != 0 && weekBeforeOpenInvoice.compareTo(BigDecimal.ZERO) != 0) {

                changeValue = ((previousDayOpenInvoice.minus(weekBeforeOpenInvoice!!)).divide(weekBeforeOpenInvoice, 5, RoundingMode.CEILING)).multiply(BigDecimal(100))
            }
        } else if (change == "creditNote") {
            if (previousDayCreditNote.compareTo(BigDecimal.ZERO) != 0 && weekBeforeCreditNote.compareTo(BigDecimal.ZERO) != 0) {
                changeValue = ((previousDayCreditNote.minus(weekBeforeCreditNote!!)).divide(weekBeforeCreditNote, 5, RoundingMode.CEILING)).multiply(BigDecimal(100))
            }
        } else {
            if (previousDayOnAccount.compareTo(BigDecimal.ZERO) != 0 && weekBeforeOnAccount.compareTo(BigDecimal.ZERO) != 0) {

                changeValue = ((previousDayOnAccount.minus(weekBeforeOnAccount!!)).divide(weekBeforeOnAccount, 5, RoundingMode.CEILING)).multiply(BigDecimal(100))
            }
        }

        return changeValue
    }

    override suspend fun uploadPayblesStats() {

        AresConstants.COGO_ENTITIES.map {
            val payblesInfo = getPayablesInfo(it)
            val currentDate = Timestamp.valueOf(LocalDateTime.now())
            val paybleStats = PayableStatsOpenSearchResponse(date = currentDate, entity = it, openInvoiceAmount = payblesInfo.openInvoicesAmount, onAccountAmount = payblesInfo.onAccountAmount, creditNoteAmount = payblesInfo.creditNoteAmount)
            Client.addDocument(AresConstants.PAYABLES_STATS_INDEX, currentDate.toString(), paybleStats, true)
        }
    }

    override suspend fun getTopTenVendors(request: SupplierOutstandingRequest): TopTenVendorsRes {
        if (request.flag == "overall") {
            throw AresException(AresError.ERR_1003, "Entity not found")
        }
        val res = listSupplierDetails(request)
        return TopTenVendorsRes(list = res.list, currency = AresConstants.LEDGER_CURRENCY.get(request.flag?.toInt()))
    }
}
