package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CustomerOutstandingAgeing
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.api.payment.entity.SupplierOutstandingAgeing
import com.cogoport.ares.model.payment.response.BillOutStandingAgeingResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.InvoicesOutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.OutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import org.mapstruct.Mapper

@Mapper
interface OutstandingAgeingMapper {
    fun convertToModel(outstandingAgeing: OutstandingAgeing): OutstandingAgeingResponse

    fun convertToEntity(outstandingAgeing: OutstandingAgeingResponse): OutstandingAgeing

    fun convertToBillModel(billOutstanding: SupplierOutstandingAgeing): BillOutStandingAgeingResponse

    fun convertToOutstandingModel(billOutstanding: SupplierOutstandingAgeing): BillOutStandingAgeingResponse

    fun convertSupplierDetailsRequestToDocument(request: SupplierOutstandingDocument): SupplierOutstandingDocument

    fun convertToInvoiceOutstandingModel(invoiceOutstanding: CustomerOutstandingAgeing): InvoicesOutstandingAgeingResponse

    fun convertCustomerDetailsRequestToDocument(request: CustomerOutstandingDocumentResponse): CustomerOutstandingDocumentResponse
}
