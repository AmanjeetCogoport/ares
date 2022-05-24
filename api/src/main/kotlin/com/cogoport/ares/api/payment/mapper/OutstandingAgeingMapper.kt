package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CustomerInvoice
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingAgeingResponse

interface OutstandingAgeingMapper {
    fun convertToModel(outstandingAgeing: OutstandingAgeing): OutstandingAgeingResponse

    fun convertToEntity(outstandingAgeing: OutstandingAgeingResponse): OutstandingAgeing
}