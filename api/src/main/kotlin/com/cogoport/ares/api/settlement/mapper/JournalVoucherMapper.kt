package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
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

    fun convertToIncidentModel(journalVoucher: JournalVoucherRequest): com.cogoport.hades.model.incident.JournalVoucher

    @Mapping(target = "id", expression = "java(null)")
    fun convertIncidentModelToEntity(jv: com.cogoport.hades.model.incident.JournalVoucher): JournalVoucher

    fun convertICJVEntityToModel(jv: ParentJournalVoucher): ParentJournalVoucherResponse
}
