package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.model.payment.InvoiceListRequest
import com.cogoport.ares.model.payment.OutstandingListRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.OutstandingAgeingResponse
import com.cogoport.ares.model.payment.OutstandingList
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import kotlin.math.ceil

@Singleton
class OutStandingServiceImpl : OutStandingService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var outstandingAgeingConverter: OutstandingAgeingMapper

    private fun validateInput(zone: String?, role: String?) {
        if (AresConstants.ROLE_ZONE_HEAD == role && zone.isNullOrBlank()) {
            throw AresException(AresError.ERR_1003, AresConstants.ZONE)
        }
    }

    override suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList {
        validateInput(request.zone, request.role)
        val queryResponse = accountUtilizationRepository.getOutstandingAgeingBucket(request.zone, request.orgName + "%", request.orgId, request.page, request.pageLimit)
        val ageingBucket = mutableListOf<OutstandingAgeingResponse>()
        val orgId = mutableListOf<String>()
        queryResponse.forEach { ageing ->
            orgId.add(ageing.organizationId!!)
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
                    val year = assignAgeingBucket("365+", ageing.threesixfiveplusAmount, ageing.threesixfiveplusCount, "365+")
                    output?.ageingBucket = listOf(zero, thirty, sixty, ninety, oneEighty, threeSixtyFive, year)
                }
            }
            listOrganization.add(output)
        }

        return OutstandingList(
            list = listOrganization,
            totalPage = ceil(total / request.pageLimit.toDouble()).toInt(),
            totalRecords = total.toInt(),
        )
    }

    override suspend fun getInvoiceList(request: InvoiceListRequest): MutableList<CustomerInvoiceResponse> {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val response = mutableListOf<CustomerInvoiceResponse>()
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
        return response
    }

    private fun assignAgeingBucket(ageDuration: String, amount: BigDecimal?, count: Int, key: String): AgeingBucket {
        return AgeingBucket(
            ageingDuration = ageDuration,
            amount = amount,
            count = count,
            ageingDurationKey = key
        )
    }
}
