package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SettledInvoiceMapper {
    @Mapping(target = "accMode", expression = "java(AccMode.AR)")
    fun convertKnockoffRequestToEntity(request: SettlementKnockoffRequest): Payment

    @Mapping(source = "destinationId", target = "documentNo")
    @Mapping(source = "destinationType", target = "documentType")
    @Mapping(source = "accType", target = "accountType")
    @Mapping(source = "ledAmount", target = "ledgerAmount")
    @Mapping(source = "documentAmount", target = "afterTdsAmount")
    @Mapping(target = "allocationAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "balanceAfterAllocation", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "settledAllocation", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "balanceAmount", expression = "java(java.math.BigDecimal.ZERO)")
    fun convertToModel(
        historyDocument: SettledInvoice?
    ): com.cogoport.ares.model.settlement.SettledInvoice
}
