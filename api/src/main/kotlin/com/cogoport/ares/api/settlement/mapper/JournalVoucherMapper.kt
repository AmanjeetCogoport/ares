package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface JournalVoucherMapper {

    fun convertToModelResponse(journalVoucher: JournalVoucher): JournalVoucherResponse

    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertRequestToEntity(journalVoucher: JournalVoucherRequest): JournalVoucher

    fun convertEntityToRequest(journalVoucher: JournalVoucher): JournalVoucherRequest

    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertToIncidentModel(journalVoucher: JournalVoucherRequest): com.cogoport.hades.model.incident.JournalVoucher

    fun convertIncidentModelToEntity(jv: com.cogoport.hades.model.incident.JournalVoucher): JournalVoucher
}
