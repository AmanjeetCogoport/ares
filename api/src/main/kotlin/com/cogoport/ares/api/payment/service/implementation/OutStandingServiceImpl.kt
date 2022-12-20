package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.response.BillOutStandingAgeingResponse
import com.cogoport.ares.model.payment.response.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.response.OutstandingAgeingResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
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
            classType = CustomerInvoiceResponse ::class.java,
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

    override suspend fun getBillsOutstandingList(request: OutstandingListRequest): OutstandingList {
        validateInput(request)
        val queryResponse = accountUtilizationRepository.getBillsOutstandingAgeingBucket(request.zone, "%" + request.query + "%", request.orgId, request.page, request.pageLimit)
        val totalRecords = accountUtilizationRepository.getBillsOutstandingAgeingBucketCount(request.zone, "%" + request.query + "%", request.orgId)
        val ageingBucket = mutableListOf<BillOutStandingAgeingResponse>()
        val listOrganization: MutableList<CustomerOutstanding?> = mutableListOf()
        queryResponse.forEach {
            it
            ageingBucket.add(outstandingAgeingConverter.convertToBillModel(it))
        }
        ageingBucket.forEach {
            var data = accountUtilizationRepository.generateOrgOutstanding(it.organizationId!!, request.zone)
            var dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
            var invoicesDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.openInvoicesAmount.toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            var paymentsDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.paymentsAmount.toString().toBigDecimal() }, it.value.sumOf { it.paymentsCount!! }) }.toMutableList()
            var outstandingDues = dataModel.groupBy { it.currency }.map { DueAmount(it.key, it.value.sumOf { it.outstandingAmount.toString().toBigDecimal() }, it.value.sumOf { it.openInvoicesCount!! }) }.toMutableList()
            var invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
            var paymentsCount = dataModel.sumOf { it.paymentsCount!! }
            var invoicesLedAmount = dataModel.sumOf { it.openInvoicesLedAmount!! }
            var paymentsLedAmount = dataModel.sumOf { it.paymentsLedAmount!! }
            var outstandingLedAmount = dataModel.sumOf { it.outstandingLedAmount!! }
            openSearchServiceImpl.validateDueAmount(invoicesDues)
            openSearchServiceImpl.validateDueAmount(paymentsDues)
            openSearchServiceImpl.validateDueAmount(outstandingDues)
            var orgId = it.organizationId
            var orgName = it.organizationName
            var orgOutstanding = CustomerOutstanding(orgId, orgName, request.zone, InvoiceStats(invoicesCount, invoicesLedAmount, invoicesDues.sortedBy { it.currency }), InvoiceStats(paymentsCount, paymentsLedAmount, paymentsDues.sortedBy { it.currency }), InvoiceStats(invoicesCount, outstandingLedAmount, outstandingDues.sortedBy { it.currency }), null)
            val zero = assignAgeingBucket("Not Due", it.notDueAmount, it.notDueCount, "not_due")
            val thirty = assignAgeingBucket("1-30", it.thirtyAmount, it.thirtyCount, "1_30")
            val sixty = assignAgeingBucket("31-60", it.sixtyAmount, it.sixtyCount, "31_60")
            val ninety = assignAgeingBucket("61-90", it.ninetyAmount, it.ninetyCount, "61_90")
            val oneEighty = assignAgeingBucket("91-180", it.oneeightyAmount, it.oneeightyCount, "91_180")
            val threeSixtyFive = assignAgeingBucket("180-365", it.threesixfiveAmount, it.threesixfiveCount, "180_365")
            val year = assignAgeingBucket("365+", it.threesixfiveplusAmount, it.threesixfiveplusCount, "365")
            orgOutstanding.ageingBucket = listOf(zero, thirty, sixty, ninety, oneEighty, threeSixtyFive, year)
            listOrganization.add(orgOutstanding)
        }

        return OutstandingList(
            list = listOrganization.sortedBy { it?.organizationName?.uppercase() },
            totalPage = ceil(totalRecords / request.pageLimit.toDouble()).toInt(),
            totalRecords = totalRecords,
            page = request.page
        )
    }
}
