package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.mapper.InvoiceMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class OutStandingServiceImpl : OutStandingService{
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository
    @Inject
    lateinit var invoiceConverter: InvoiceMapper
    override suspend fun getOutstandingList(zone: String?, role: String?): OutstandingList? {
        var ageingData = accountUtilizationRepository.getOutstandingAgeing()
    }

    override suspend fun getInvoiceList(zone: String?, orgId: String?, page: Int , page_limit: Int): MutableList<CustomerInvoiceResponse> {
        var offset = (page_limit!! * page) - page_limit
        var invoicesList = accountUtilizationRepository.fetchInvoice(zone,orgId, offset, page_limit)
        var invoice = mutableListOf<CustomerInvoiceResponse>()
        invoicesList.forEach { invoices ->
            run { invoice.add(invoiceConverter.convertToModel(invoices)) }
        }
        return invoice
    }
}
