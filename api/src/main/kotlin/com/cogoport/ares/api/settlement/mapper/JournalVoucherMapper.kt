package com.cogoport.ares.api.settlement.mapper
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import org.mapstruct.Mapper

@Mapper
interface JournalVoucherMapper {

    fun convertToModel(journalVoucher: JournalVoucher): JournalVoucherResponse

    fun convertToEntity(JournalVoucherResponse: JournalVoucherResponse): JournalVoucher
}
