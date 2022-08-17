package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface JournalVoucherMapper {

    fun convertToModelResponse(journalVoucher: JournalVoucher): JournalVoucherResponse

    fun convertRequestToEntity(journalVoucher: com.cogoport.ares.model.settlement.request.JournalVoucher): JournalVoucher

    fun convertEntityToRequest(journalVoucher: JournalVoucher): com.cogoport.ares.model.settlement.request.JournalVoucher

    fun convertToIncidentModel(journalVoucher: com.cogoport.ares.model.settlement.request.JournalVoucher): com.cogoport.hades.model.incident.JournalVoucher
}
