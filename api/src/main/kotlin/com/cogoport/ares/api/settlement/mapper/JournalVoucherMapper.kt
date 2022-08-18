package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface JournalVoucherMapper {

    fun convertToModelResponse(journalVoucher: JournalVoucher): JournalVoucherResponse

    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertRequestToEntity(journalVoucher: com.cogoport.ares.model.settlement.request.JournalVoucherRequest): JournalVoucher

    fun convertEntityToRequest(journalVoucher: JournalVoucher): com.cogoport.ares.model.settlement.request.JournalVoucherRequest

    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertToIncidentModel(journalVoucher: JournalVoucher): com.cogoport.hades.model.incident.JournalVoucher
}
