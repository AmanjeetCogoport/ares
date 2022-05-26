package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.mapper.InvoiceMapper
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.payment.*
import com.cogoport.brahma.opensearch.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal

@Singleton
class OutStandingServiceImpl : OutStandingService{
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository
    @Inject
    lateinit var invoiceConverter: InvoiceMapper
    @Inject
    lateinit var outstandingAgeingConverter: OutstandingAgeingMapper
    override suspend fun getOutstandingList(zone: String?, role: String?, quarter: Int?, page: Int, page_limit: Int): OutstandingList? {
        val queryResponse = accountUtilizationRepository.getOutstandingAgeingBucket(zone, page, page_limit)
        val outstandings = mutableListOf<OutstandingAgeingResponse>()
        val orgId = mutableListOf<String>()
        queryResponse.forEach {outstanding ->
            orgId.add(if(zone.isNullOrBlank()) outstanding.organization_id + "_all" + "_Q$quarter" else outstanding.organization_id + "_" + zone + "_Q$quarter")
            run { outstandings.add(outstandingAgeingConverter.convertToModel(outstanding)) }
        }
        val response = OpenSearchClient().listApi(
            index= AresConstants.SALES_OUTSTANDING_INDEX, classType= CustomerOutstanding::class.java, values = orgId
        )
        val data: MutableList<CustomerOutstanding?> = mutableListOf()
        for (hts in response?.hits()?.hits()!!) {
            val output: CustomerOutstanding? = hts.source()
            for (item in outstandings){
                if(item.organization_id == output?.organizationId){
                    val zero = assignAgeingBucket("Not Due",item.not_due_amount,item.not_due_count,"not_due")
                    val thirty = assignAgeingBucket("1-30",item.thirty_amount,item.thirty_count,"1_30")
                    val sixty = assignAgeingBucket("31-60",item.sixty_amount,item.sixty_count,"31_60")
                    val ninety = assignAgeingBucket("61-90",item.ninety_amount,item.ninety_count,"61_90")
                    val oneEighty = assignAgeingBucket("91-180",item.oneeighty_amount,item.oneeighty_count,"91_180")
                    val threeSixtyFive = assignAgeingBucket("180-365",item.threesixfive_amount,item.threesixfive_count,"180_365")
                    val year = assignAgeingBucket("365+",item.threesixfiveplus_amount,item.threesixfiveplus_count,"365+")
                    output.ageingBucket = listOf(zero,thirty,sixty,ninety,oneEighty,threeSixtyFive,year)
                }
            }
            data.add(output)
        }



        return OutstandingList(
            organizationList = data,
            totalPage = data.size,
            totalRecords = 10,
        )
    }

    override suspend fun getInvoiceList(zone: String?, orgId: String?, page: Int, page_limit: Int): MutableList<CustomerInvoiceResponse>? {
        val offset = (page_limit * page) - page_limit
        val invoicesList = accountUtilizationRepository.fetchInvoice(zone, orgId, page, page_limit)

        invoicesList.forEach {
            Client.updateDocument("customer_invoice_index", it.invoiceNumber.toString() ,it)
        }

        val response = mutableListOf<CustomerInvoiceResponse>()
        if (orgId != null) {
            val searchValues = mutableListOf<String>()
            if (zone != null) {
                searchValues.add(zone)
            }

            searchValues.add(orgId)

             val list : SearchResponse<CustomerInvoiceResponse>? = OpenSearchClient().response(
                searchKey = orgId,
                classType = CustomerInvoiceResponse ::class.java,
                index = "customer_invoice_index",
                offset = page ,
                limit = page_limit
             )
            list?.hits()?.hits()?.map {
                it.source()?.let { it1 -> response.add(it1) }
            }
            return  response
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
