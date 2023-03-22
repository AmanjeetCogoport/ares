package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.request.ICJVRequest
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.ParentICJVRequest
import com.cogoport.hades.model.incident.response.ICJVEntry
import com.cogoport.hades.model.incident.response.InterCompanyJournalVoucher
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface JournalVoucherMapper {

    @Mapping(target = "category", expression = "java(journalVoucher.getCategory().getValue())")
    fun convertToModelResponse(journalVoucher: JournalVoucher): JournalVoucherResponse

    fun convertToModelResponse(journalVoucher: List<JournalVoucher>): List<JournalVoucherResponse>

    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertRequestToEntity(journalVoucher: JournalVoucherRequest): JournalVoucher

    fun convertEntityToRequest(journalVoucher: JournalVoucher): JournalVoucherRequest

    fun convertToIncidentModel(journalVoucher: JournalVoucherRequest): com.cogoport.hades.model.incident.JournalVoucher

    @Mapping(target = "id", expression = "java(null)")
    fun convertIncidentModelToEntity(jv: com.cogoport.hades.model.incident.JournalVoucher): JournalVoucher

    fun convertICJVEntityToModel(jv: ParentJournalVoucher): ParentJournalVoucherResponse

    @Mapping(target = "ledCurrency", constant = "INR")
    fun convertICJVRequestToJournalVoucher(icjv: ICJVRequest): JournalVoucher

    @Mapping(target = "gLCode", source = "glCode")
    fun convertJournalVoucherModelToICJVEntry(icjv: JournalVoucher): ICJVEntry

    @Mapping(target = "list", expression = "java(kotlin.collections.CollectionsKt.emptyList())")
    @Mapping(source = "validityDate", target = "validityDate", dateFormat = "yyyy-MM-dd")
    fun convertParentJVToICJVApproval(parentJournalVoucher: ParentJournalVoucher): InterCompanyJournalVoucher

    @Mapping(source = "createdAt", target = "updatedAt")
    @Mapping(source = "createdBy", target = "updatedBy")
    fun convertParentICJVRequestToParentJV(parentICJVRequest: ParentICJVRequest): ParentJournalVoucher

    fun convertParentJVModelToJournalVoucherResponse(parentJournalVoucher: ParentJournalVoucher): JournalVoucherResponse

    fun convertParentJVModelToJournalVoucherResponse(parentJournalVoucher: List<ParentJournalVoucher>): List<JournalVoucherResponse>
}
