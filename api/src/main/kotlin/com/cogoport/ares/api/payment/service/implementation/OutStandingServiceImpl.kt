package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.model.OutstandingListRequest
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

@Singleton
class OutStandingServiceImpl : OutStandingService {
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository
    @Inject
    lateinit var outstandingAgeingConverter: OutstandingAgeingMapper
    override suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList {
        val queryResponse = accountUtilizationRepository.getOutstandingAgeingBucket(request.zone, request.orgName + "%",request.page, request.page_limit)
        val ageingBucket = mutableListOf<OutstandingAgeingResponse>()
        val orgId = mutableListOf<String>()
        queryResponse.forEach { ageing ->
            orgId.add(if (request.zone.isNullOrBlank()) ageing.organization_id + AresConstants.KEY_DELIMITER + "ALL" else ageing.organization_id + AresConstants.KEY_DELIMITER + request.zone)
            ageingBucket.add(outstandingAgeingConverter.convertToModel(ageing))
        }
        val response = OpenSearchClient().listApi(index = AresConstants.SALES_OUTSTANDING_INDEX, classType = CustomerOutstanding::class.java, values = orgId)
        val listOrganization: MutableList<CustomerOutstanding?> = mutableListOf()
        for (hts in response?.hits()?.hits()!!) {
            val output: CustomerOutstanding? = hts.source()
            for (ageing in ageingBucket) {
                if (ageing.organization_id == output?.organizationId) {
                    val zero = assignAgeingBucket("Not Due", ageing.not_due_amount, ageing.not_due_count, "not_due")
                    val thirty = assignAgeingBucket("1-30", ageing.thirty_amount, ageing.thirty_count, "1_30")
                    val sixty = assignAgeingBucket("31-60", ageing.sixty_amount, ageing.sixty_count, "31_60")
                    val ninety = assignAgeingBucket("61-90", ageing.ninety_amount, ageing.ninety_count, "61_90")
                    val oneEighty = assignAgeingBucket("91-180", ageing.oneeighty_amount, ageing.oneeighty_count, "91_180")
                    val threeSixtyFive = assignAgeingBucket("180-365", ageing.threesixfive_amount, ageing.threesixfive_count, "180_365")
                    val year = assignAgeingBucket("365+", ageing.threesixfiveplus_amount, ageing.threesixfiveplus_count, "365+")
                    output.ageingBucket = listOf(zero, thirty, sixty, ninety, oneEighty, threeSixtyFive, year)
                }
            }
            listOrganization.add(output)
        }

        return OutstandingList(
            organizationList = listOrganization,
            totalPage = listOrganization.size,
            totalRecords = 10,
        )
    }

    override suspend fun getInvoiceList(zone: String?, orgId: String?, page: Int, page_limit: Int): MutableList<CustomerInvoiceResponse>? {
        val offset = (page_limit * page) - page_limit
//        val invoicesList = accountUtilizationRepository.fetchInvoice(zone, orgId, page, page_limit)

//        invoicesList.forEach {
//            Client.updateDocument("index_ares_invoice_outstanding", it.invoiceNumber.toString() ,it)
//        }

        val response = mutableListOf<CustomerInvoiceResponse>()

        val list: SearchResponse<CustomerInvoiceResponse>? = OpenSearchClient().responseList(
            searchKey = orgId,
            classType = CustomerInvoiceResponse ::class.java,
            index = AresConstants.INVOICE_OUTSTANDING_INDEX,
            offset = offset,
            limit = page_limit
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
