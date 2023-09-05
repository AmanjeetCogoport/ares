package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.SupplierLevelData
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocumentV2
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SupplierOrgOutstandingMapper {

    @Mapping(target = "tradeType", expression = "java(null)")
    @Mapping(target = "agent", expression = "java(null)")
    @Mapping(target = "totalOpenInvoiceCount", expression = "java(0l)")
    @Mapping(target = "totalOpenOnAccountCount", expression = "java(0l)")
    @Mapping(target = "creditDays", expression = "java(0l)")
    fun convertToModel(orgOutstanding: SupplierLevelData): SupplierOutstandingDocumentV2

    fun convertToEntity(req: SupplierOutstandingDocumentV2): SupplierLevelData
}