package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CustomerInvoice
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import org.mapstruct.Mapper

@Mapper
interface InvoiceMapper {
    fun convertToModel(customerInvoice: CustomerInvoice): CustomerInvoiceResponse

    fun convertToEntity(customerInvoice: CustomerInvoiceResponse): CustomerInvoice
}
