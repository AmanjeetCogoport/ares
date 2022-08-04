package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface JournalVoucherMapper {

    fun convertToModel(journalVoucher: JournalVoucher): JournalVoucherResponse

    fun convertToEntity(JournalVoucherResponse: JournalVoucherResponse): JournalVoucher
}
