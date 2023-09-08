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
    @Mapping(source = "closingInvoiceBalance2022", target = "closingInvoiceAmountAtFirstApril")
    @Mapping(source = "closingOnAccountBalance2022", target = "closingOnAccountAmountAtFirstApril")
    @Mapping(source = "closingOutstanding2022", target = "closingOutstandingAmountAtFirstApril")
    fun convertToModel(orgOutstanding: SupplierLevelData): SupplierOutstandingDocumentV2

    fun convertToEntity(req: SupplierOutstandingDocumentV2): SupplierLevelData
}
