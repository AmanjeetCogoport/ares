package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.BillOutstandingAgeing
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.model.payment.response.BillOutStandingAgeingResponse
import com.cogoport.ares.model.payment.response.OutstandingAgeingResponse
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import org.mapstruct.Mapper

@Mapper
interface OutstandingAgeingMapper {
    fun convertToModel(outstandingAgeing: OutstandingAgeing): OutstandingAgeingResponse

    fun convertToEntity(outstandingAgeing: OutstandingAgeingResponse): OutstandingAgeing

    fun convertToBillModel(billOutstanding: BillOutstandingAgeing): BillOutStandingAgeingResponse

    fun convertToOutStandingModel(billOutstanding: BillOutstandingAgeing): BillOutStandingAgeingResponse

    fun convertSupplierOutstandingRequestToDocument(request: SupplierOutstandingDocument): SupplierOutstandingDocument
}
